import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class CommunicateHelper {

    static final int PACKAGE_SIZE = 1024;
    static final ArrayList<String> typeField = new ArrayList<String>(
            Arrays.asList("Sports", "Lifestyle", "Entertainment", "Business", "Technology", "Science", "Politics", "Health"));


    public static String generateSubRequest(String publishedArticle){
        StringBuilder generateString
                = new StringBuilder("");
        String[] fieldValues = publishedArticle.split(";");
        for(int i = 0; i < 3; i++) {
            generateString.append(fieldValues[i]);
            generateString.append(";");
        }
        return generateString.toString();
    }

    public static Boolean checkAllCases(String toCheck, String article){

        String[] toCheckValues = toCheck.split(";");
        String[] articleValues = article.split(";");
        // No match only if not null and mismatch
        for(int i=0;i< toCheckValues.length;i++){
            if (articleValues[i]!="" && !(toCheckValues[i].equalsIgnoreCase(articleValues[i]))){
                return false;
            }
        }
        return true;
    }

    public static Boolean isClientSubscribed(ArrayList<String> clientSubscribedArticles, String toCheck){
        for(int i=0;i<clientSubscribedArticles.size();i++){
            if(checkAllCases(clientSubscribedArticles.get(i),toCheck))
                return true;
        }
        return false;
    }

    public static ArrayList<String> getListOfClients(Map<String, ArrayList<String>> clientSubscriptionList, String article){
        ArrayList<String> clientList;
        clientList = new ArrayList<>();
        String toCheck = generateSubRequest(article);
        Object[] keys =  clientSubscriptionList.keySet().toArray();
        for(int i=0;i<keys.length;i++){
            if(isClientSubscribed(clientSubscriptionList.get(keys[i]),toCheck)) {
                //Adding IP of client to the list
                clientList.add(String.valueOf(clientSubscriptionList.keySet().toArray()[i]));
            }
        }

        return clientList;
    }

    public static String udpToAndFromRemoteServer(String message, String ip) {
        byte[] buf = new byte[1024];
        DatagramSocket ds = udpToRemoteServer(message, ip);
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        try {
            ds.receive(dp);
            String received
                    = new String(dp.getData(), 0, dp.getLength());
            System.out.print(received);
            return received;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DatagramSocket udpToRemoteServer(String message, String ip) {
        try {
            DatagramSocket ds = new DatagramSocket();
            byte[] b = message.getBytes();

            /* Hard coding the public IP of registry server here. This is
                to send UDP messages to that IP at port 5105
            * */
            //InetAddress ir = InetAddress.getByName("134.84.182.49");
            InetAddress ir = InetAddress.getByName(ip);
            DatagramPacket dp = new DatagramPacket(b, b.length, ir, 5105);
            ds.send(dp);
            return ds;
        } catch (SocketException | UnknownHostException e) {
            System.out.println("Socket Exception trying to connect to Remote Server");
        } catch (IOException e) {
            System.out.println("Couldn't send the package to the Remote Server");
        }
        return null;
    }

    public static void udpToClients(List<String> subscribers, Map<String, Integer> portLookup, String message) {
        try {
            DatagramSocket ds = new DatagramSocket();
            byte[] b = new byte[PACKAGE_SIZE];
            b = message.getBytes();

            for(int i=0;i<subscribers.size();i++){
                InetAddress address = InetAddress.getByName(subscribers.get(i));

                System.out.println("Sending message: " + message + " to " + subscribers.get(i) + " at port " + portLookup.get(subscribers.get(i)));
                DatagramPacket dp = new DatagramPacket(b,b.length,address,portLookup.get(subscribers.get(i)));
                ds.send(dp);
            }
        } catch (SocketException | UnknownHostException e) {
            System.out.println("Socket error while sending UDP packets to Clients");
        } catch (IOException e) {
            System.out.println("IO error while sending packets to clients");
        }

    }

    public static String getMessage(String article) {
        if(article.length() >120) {
            article = article.substring(0,120);
        }
        else if (article.length() < 120) {
            article = String.format("%1$-120s", article);
        }
        String[] fieldValues = article.split(";");
        return fieldValues[fieldValues.length-1];

    }

    public static Boolean validateString(String article) {
        int count = 0;
        String[] stringToCheck = article.split(";");
        if(stringToCheck.length <= 0)
            return false;
        if(stringToCheck.length > 3) {
            if(stringToCheck[3].trim() != "")
                return false;
        }
        if(!typeField.contains(stringToCheck[0].trim()))
            return false;
        for(String string: stringToCheck){
            count += string.trim().length();
        }
        if(count == 0)
            return false;
        return true;
    }

    public static Boolean validatePublisherString(String article) {
        String[] stringToCheck = article.split(";");
        if(stringToCheck.length <= 0)
            return false;
        if(stringToCheck.length == 4){
            if(stringToCheck[3].trim().length() == 0)
                return false;
            if(stringToCheck[0].trim() == "" && stringToCheck[1].trim() == "" && stringToCheck[2].trim() == "")
                return false;
        }
        else
            return false;

        return true;
    }

    public static String getIP() throws UnknownHostException, MalformedURLException {
        InetAddress ia = InetAddress.getLocalHost();

        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        String ip="";
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            ip= in.readLine();
        } catch (IOException e) {
            System.out.println("Public IP not working properly. ");
        }

        System.out.println("My public IP is " + ip);
        return ip;
    }

    public static Map<String,Integer> getServerList( String ipRegistry, String ip) throws UnknownHostException, MalformedURLException {
        StringBuilder getListRequest = new StringBuilder("");
        getListRequest.append("GetList;RMI;");
        System.out.println("Registry Service" + ipRegistry);

        getListRequest.append(ip);
        getListRequest.append(";1098");

        String ipAddressOfGSrvs = CommunicateHelper.udpToAndFromRemoteServer(getListRequest.toString(),ipRegistry);
        String [] splitAdresses = ipAddressOfGSrvs.split(";");
        Map<String,Integer>ipAddrAndPort = new HashMap<>();
        for(int i = 0; i < splitAdresses.length; i=i+2) {
            if(i+1 >= splitAdresses.length ) continue;
            ipAddrAndPort.put(splitAdresses[i],Integer.valueOf(splitAdresses[i+1]));
        }
        return ipAddrAndPort;
    }

    public static Communicate connectToGroupServer(Map<String,Integer>  ipAddrAndPort, String ip) throws RemoteException, NotBoundException, UnknownHostException {
        System.out.println("I am done - Client");
        boolean joinAllowed = false;
        int index = 0;

        Communicate comm = null;
        while (index < ipAddrAndPort.size()) {
            String serverIP = (String) ipAddrAndPort.keySet().toArray()[index];
            int port = ipAddrAndPort.get(serverIP);
            Registry registry = LocateRegistry.getRegistry(serverIP);
            comm = (Communicate) registry.lookup("server.comm");

            boolean isJoin = comm.join(ip, 1098);
            if(!isJoin){
                index++;
            }
            else {
                System.out.println("Joining at " + ip + "listening to port " + 1098);
                break;
            }
        }
        return comm;
    }

}
