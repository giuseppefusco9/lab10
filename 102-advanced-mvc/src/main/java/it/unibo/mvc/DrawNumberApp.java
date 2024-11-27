package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final String ATTEMPTS_STRING = "attempts";
    private static final String MAXIMUM_STRING = "maximum";
    private static final String MINIMUM_STRING = "minimum";
    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views the views to attach
     * @param fileName the name of the file to open
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public DrawNumberApp(final String fileName, final DrawNumberView... views) throws FileNotFoundException, IOException {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Configuration values = dataFromFile(fileName).build();
        this.model = new DrawNumberImpl(values.getMin(), values.getMax(), values.getAttempts());
    }

    private Configuration.Builder dataFromFile(final String fileName) {
        final Configuration.Builder retValue = new Configuration.Builder();
        try (var reader = new BufferedReader(
                new InputStreamReader(
                    ClassLoader.getSystemResourceAsStream(fileName), StandardCharsets.UTF_8))) {
            for (var line = reader.readLine(); line != null; line = reader.readLine()) {
                final StringTokenizer st = new StringTokenizer(line, ": ");
                final String element = st.nextToken();
                switch (element) {
                    case MINIMUM_STRING -> retValue.putMin(Integer.parseInt(st.nextToken(": ")));
                    case MAXIMUM_STRING -> retValue.putMax(Integer.parseInt(st.nextToken(": ")));
                    case ATTEMPTS_STRING -> retValue.putAttempts(Integer.parseInt(st.nextToken(": ")));
                    default -> displayViewsError("No values for " + element + " found");
                }
            }
        } catch (NumberFormatException e) {
            displayViewsError("Invalid number format in configuration file: " + e.getMessage());
        } catch (IOException e) {
            displayViewsError("Error reading configuration file: " + e.getMessage());
        }
        return retValue;
    }

    private void displayViewsError(final String msg) {
        for (final DrawNumberView view : views) {
            view.displayError(msg);
        }
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        //System.exit(0);
    }

    /**
     * @param args ignored
     * @throws IOException 
    */
     public static void main(final String... args) throws IOException {
        new DrawNumberApp("config.yml", new DrawNumberViewImpl(), 
            new DrawNumberViewImpl(), 
            new PrintStreamView(System.out), 
            new PrintStreamView("output.log"));
    }
}
