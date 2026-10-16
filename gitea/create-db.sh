export PGPASSWORD=gitea
/opt/bitnami/postgresql/bin/createdb -U gitea gitea || true
/opt/bitnami/postgresql/bin/psql -U gitea -c "alter user gitea with encrypted password 'gitea';"
/opt/bitnami/postgresql/bin/psql -U gitea -c "grant all privileges on database gitea to gitea;"
/opt/bitnami/postgresql/bin/psql -d gitea -U gitea -c "create schema gitea;"
