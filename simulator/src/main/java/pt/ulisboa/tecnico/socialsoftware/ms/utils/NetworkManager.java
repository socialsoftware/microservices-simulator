package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import java.util.Random;

public class NetworkManager {
    private static NetworkManager instance;
    private static String directory;
    private Random random = new Random();
    private boolean loaded = false;

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public static void setDirectory(String dir) {
        directory = dir;
    }

    public void load() {
        if (loaded) {
            return;
        }

        // ! TODO - implement

        loaded = true;
    }

    public int generateDelay(String microservice, String endpoint) {
        // TODO - change weight calculation
        // TODO - vary parameters
        int distanceWeight = 1;
        double mu = 3.0; // median delay e^mu (3 - 20ms)
        double sigma = 0.5; // distribution

        // Base delay generated via Log-Normal distribution
        double baseDelay = Math.exp(mu + sigma * random.nextGaussian());

        return (int) (baseDelay * distanceWeight);
    }

    public int generateFault() {
        // TODO - Implement this to randomly generate faults at runtime
        return 0;
    }
}
