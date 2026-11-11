package poc.citrus.synthetic;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for the citrus-rmi round trip: the Citrus
 * {@code RmiServer} binds a proxy for this interface into its
 * (created) registry and the {@code RmiClient} invokes it.
 */
public interface NewsService extends Remote {

    String getNews() throws RemoteException;

    void setNews(String news) throws RemoteException;
}
