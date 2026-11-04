Prerequisite: `OPENAI_HOST` must be set (used as the default destination by the helper), e.g.
```bash
export OPENAI_HOST=maas.apps.ocp.cloud.rhai-tmm.dev
```
Create the mtu-test pod
```bash
oc apply -f mtest-pod.yaml
oc wait --for=condition=Ready pod/mtu-test --timeout=120s
```
Find which hop caps the size
The helper runs bounded tracepath (starting at the interface MTU) and analyzes
the output: identifies the hop that lowers the PMTU, unanswered hops
(firewalls), and whether the destination was reached. It then re-runs
tracepath at the lowest MTU identified to confirm the path is clean there.
If it ends before the destination, rerun with `MAX_HOPS=30` rather than
assuming nothing caps the path.
```bash
./analyze-tracepath.sh
```
Delete the mtu-test pod
```bash
oc delete pod mtu-test
```

## Keycloak: firstName/lastName required in the user profile

The MTA realm's user profile declares `firstName` and `lastName` as required
attributes (`"required": {"roles": ["user"]}`), so users are forced to fill
them in when completing their Keycloak profile. The `KeycloakRealmImport`
manifest (`mta-ui/mta-realm-import.yaml`) cannot carry the user profile
configuration — in Keycloak 26 the declarations live in a separate store
managed via the `/users/profile` admin endpoint, not part of the
`RealmRepresentation` — so the change must be applied to the running realm and
re-applied after a cluster rebuild.

To check the current configuration, query the Admin REST API from the
`mta-ui` pod (the Keycloak pod has no `curl`, and `kcadm.sh --insecure` does
not bypass the PKIX trust check for the self-signed serving cert):

```bash
KC_PASS=$(oc get secret mta-keycloak-rhbk -n openshift-mta -o jsonpath='{.data.password}' | base64 -d)
oc exec deploy/mta-ui -n openshift-mta -- bash -c "
TOKEN=\$(curl -sk https://mta-rhbk-service.openshift-mta.svc:8443/auth/realms/master/protocol/openid-connect/token -d grant_type=password -d client_id=admin-cli -d username=admin -d password='$KC_PASS' | sed -n 's/.*\"access_token\":\"\([^\"]*\)\".*/\1/p');
curl -sk -H \"Authorization: Bearer \$TOKEN\" 'https://mta-rhbk-service.openshift-mta.svc:8443/auth/admin/realms/mta/users/profile'"
```

Each attribute carries a `required` field with the roles it is mandatory for.
Remove the flag from `firstName`/`lastName` by fetching the config, deleting
their `required` fields with `jq`, and PUT-ing the full object back:

```bash
KC_PASS=$(oc get secret mta-keycloak-rhbk -n openshift-mta -o jsonpath='{.data.password}' | base64 -d)
oc exec deploy/mta-ui -n openshift-mta -- bash -c "
TOKEN=\$(curl -sk https://mta-rhbk-service.openshift-mta.svc:8443/auth/realms/master/protocol/openid-connect/token -d grant_type=password -d client_id=admin-cli -d username=admin -d password='$KC_PASS' | sed -n 's/.*\"access_token\":\"\([^\"]*\)\".*/\1/p');
curl -sk -H \"Authorization: Bearer \$TOKEN\" 'https://mta-rhbk-service.openshift-mta.svc:8443/auth/admin/realms/mta/users/profile'" \
| jq '.attributes |= map(if .name == "firstName" or .name == "lastName" then del(.required) else . end)' \
| oc exec deploy/mta-ui -n openshift-mta -i -- bash -c "
TOKEN=\$(curl -sk https://mta-rhbk-service.openshift-mta.svc:8443/auth/realms/master/protocol/openid-connect/token -d grant_type=password -d client_id=admin-cli -d username=admin -d password='$KC_PASS' | sed -n 's/.*\"access_token\":\"\([^\"]*\)\".*/\1/p');
curl -sk -X PUT -H \"Authorization: Bearer \$TOKEN\" -H 'Content-Type: application/json' --data @- -o /dev/null -w 'PUT HTTP %{http_code}\n' 'https://mta-rhbk-service.openshift-mta.svc:8443/auth/admin/realms/mta/users/profile'"
```

The `PUT` is a full-object update — the fetched config is modified and written
back rather than a partial patch. Verify with the `GET` above: the
`firstName` and `lastName` attributes should no longer carry a `required`
field, while `username`/`email` keep their existing settings.
