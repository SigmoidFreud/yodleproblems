package yodlejugglepuzzle;



import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;


public class yodle {

    public static void main(String[] args) throws IOException {
        File f = null;
        f = new File("jugglefest.txt");
        ArrayList<Circuit> circuits = new ArrayList<Circuit>();
        ArrayList<Juggler> jugglers = new ArrayList<Juggler>();
        //parses input
        InputOutputParser.initializeLists(f, circuits, jugglers);
        // returns a matcher object via the stable matching algorithm
        ModifiedStableMatcher matcher = new ModifiedStableMatcher(circuits, jugglers);
        matcher.match();
        InputOutputParser.output("output.txt", matcher);
        if (args.length == 0 || f.getName().equals("jugglefest.txt")) {
            System.out.println("Answer as per specification: " + matcher.getJugglerSum(1970));
        }
    }
}

/**
 * parser class to read input
 */
class InputOutputParser {

    
    static void initializeLists(File f, ArrayList<Circuit> circuits, ArrayList<Juggler> jugglers) throws FileNotFoundException, IOException {
        BufferedReader in;
        in = new BufferedReader(new FileReader(f));
        Circuit circuit;

        try {
            while (in.ready()) {
                String[] tokens = in.readLine().split("\\s");
                switch (tokens[0]) {
                    case "J": {
                        int jugglerID = Integer.valueOf(tokens[1].substring(1));
                        int handEye = Integer.valueOf(tokens[2].substring(2));
                        int endurance = Integer.valueOf(tokens[3].substring(2));
                        int pizzazz = Integer.valueOf(tokens[4].substring(2));
                        String[] prefTokens = tokens[5].split(",");
                        int[] preferences = new int[prefTokens.length];
                        int[] scores = new int[preferences.length];
                        for (int prefIndex = 0; prefIndex < preferences.length; prefIndex++) {
                            preferences[prefIndex] = Integer.valueOf(prefTokens[prefIndex].substring(1));
                            circuit = circuits.get(preferences[prefIndex]);
                            //score metric calculation
                            scores[prefIndex] = handEye * circuit.getHandEye() + endurance * circuit.getEndurance() + pizzazz * circuit.getPizzazz(); 
                        }
                        jugglers.add(new Juggler(jugglerID, handEye, endurance, pizzazz, preferences, scores));
                        break;
                    }
                    case "C": {
                        int circuitID = Integer.valueOf(tokens[1].substring(1));
                        int handEye = Integer.valueOf(tokens[2].substring(2));
                        int endurance = Integer.valueOf(tokens[3].substring(2));
                        int pizzazz = Integer.valueOf(tokens[4].substring(2));
                        circuits.add(new Circuit(circuitID, handEye, endurance, pizzazz));
                        break;
                    }
                }


            }
        } finally {
            in.close();
        }
    }

    static public void output(String fileName, ModifiedStableMatcher matcher) throws FileNotFoundException, IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName))) {
            writer.write(matcher.toString());
        }
    }
}

/**
 * variant of the stable matching problem call the college admissions problem where the colleges in this case circuits can accept up to the alloted team size depending upon the input paramaters
 * so the team size i.e. the number of acceptances is decided upon entering the input
 */
class ModifiedStableMatcher {

    private int circuitsSize;                                                        // Number of Circuits to be matched
    private int jugglersSize;                                                        // Number of Jugglers to be matched
    private int jugglersPerCircuit;                                                        // Number of Jugglers per Circuit
    private ArrayList<Circuit> myCircuits;
    private ArrayList<Juggler> unassignedJugglers;

    //instantiates the matcher object
    public ModifiedStableMatcher(ArrayList<Circuit> circuits, ArrayList<Juggler> jugglers) {
        myCircuits = circuits;
        unassignedJugglers = jugglers;
        circuitsSize = circuits.size();
        jugglersSize = jugglers.size();
        jugglersPerCircuit = jugglersSize / circuitsSize;
    }
    
    //Computes the preference metric using the conventional dot product
    
    public static int DotProduct(Circuit circuit, Juggler juggler) { 
        return juggler.getHandEye() * circuit.getHandEye() + juggler.getEndurance() * circuit.getEndurance() + juggler.getPizzazz() * circuit.getPizzazz();
    }

    /**
     * Matches jugglers to each circuit. Uses stable matching algorithm as described in Algorithm Design text
     */
    public void match() {

        //initializes in order to start the algorithm

        Juggler currentJuggler;
        Circuit currentCircuit;
        int preferenceIndex;
        int[] preferences, scores;
        int preferencesLength; //this comes as part of the proof of correctness in order to ensure an optimal solution is achieved (there may be more than one depending on the finer details of the algorithm)
        boolean isMatched;

        //run through the stable matching algorithm given on wikipedia and modify it so that each circuit, the wives in this case can take on a preferential list of husbands, the jugglers in this case

        while (!unassignedJugglers.isEmpty()) {
            for (int unassignedIndex = 0; unassignedIndex < unassignedJugglers.size(); unassignedIndex++) { //cannot optimize because unassignedJugglers.size() changes after every modification
                currentJuggler = unassignedJugglers.get(unassignedIndex);
                isMatched = false;
                preferenceIndex = currentJuggler.getIndex() + 1;
                preferences = currentJuggler.getPreferences();
                preferencesLength = preferences.length;
                scores = currentJuggler.getScores();
                
                //iterate through all circuits in a juggler's preference list
                for(;preferenceIndex < preferencesLength;) {
                    currentCircuit = myCircuits.get(preferences[preferenceIndex]);
                    currentJuggler.setCurrentCircuit(currentCircuit.getID()); //associate the juggler with the current circuit
                    currentJuggler.setCurrentScore(scores[preferenceIndex]);//give the juggler a score
                    currentJuggler.setIndex(preferenceIndex);

                    //if the juggler's preference is this circuit and there is room on the team assign the juggler to the team
                    if (currentCircuit.getJugglers().size() < jugglersPerCircuit) {
                        currentJuggler.setMatched(true);
                        currentCircuit.addJuggler(currentJuggler);
                        unassignedJugglers.remove(unassignedIndex);
                        isMatched = true;
                        break;

                    } //finding the weakest link and removing him/her
                    else if (currentJuggler.getCurrentScore() > currentCircuit.getMinScore()) {
                        unassignedJugglers.add(currentCircuit.replaceWeakestJuggler(currentJuggler));
                        unassignedJugglers.remove(unassignedIndex);
                        isMatched = true;
                        break;
                    } else //if neither is possible, go to the next circuit on the juggler's preference list
                    {
                        preferenceIndex++;
                    }
                }
                //if the juggler is still unassigned, loop through the list until an opening is found, then put the juggler there
                if (!isMatched) {
                    for(Circuit circuit : myCircuits) {
                        currentJuggler.setCurrentCircuit(circuit.getID());
                        currentJuggler.setCurrentScore(ModifiedStableMatcher.DotProduct(circuit, unassignedJugglers.get(unassignedIndex)));

                        //if the circuit is not filled, put the juggler there
                        if (circuit.getJugglers().size() < jugglersPerCircuit) {
                            currentJuggler.setMatched(true);
                            circuit.addJuggler(currentJuggler);
                            unassignedJugglers.remove(unassignedIndex);
                            break;
                            //else if this juggler has a higher score than the circuit's minimum score, replace that circuit's Weakest juggler
                        } else if (currentJuggler.getCurrentScore() > circuit.getMinScore()) {
                            unassignedJugglers.add(circuit.replaceWeakestJuggler(currentJuggler));
                            unassignedJugglers.remove(unassignedIndex);
                            break;
                        }
                    }
                }
            }
        }
        }

    /**
     * Used to find the output as per Yodle's specifications
     *
     */
    public int getJugglerSum(int circuitID) {
        int sum = 0;
        Circuit circuit = myCircuits.get(circuitID);
        for (Juggler j : circuit.getJugglers()) {
            sum += j.getID();
        }
        return sum;
    }

    /**
     * String representation of the circuits and jugglers
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        //String lineSeparator = System.getProperty("\n");
        for (int index = circuitsSize - 1; index >= 0; index--) {
            Circuit c = myCircuits.get(index);
            builder.append(c).append(lineSeparator);
        }
        return builder.toString();
    }
}

/**
 * Juggler design from the problem and any extra attributes required by the matching algorithm
 */
class Juggler implements Comparable<Object> {

    private boolean isMatched;
    private int ID;
    private int HandEye;
    private int Endurance;
    private int Pizzazz;
    private int[] Preferences;
    private int[] Scores;
    private int CurrentCircuit;
    private int CurrentScore;
    private int Index;
    private static final int NONE = -1; //this exists to denote freesapace on a team

    /**
     * creates the juggler object
     */
    public Juggler(int id, int handEye, int endurance, int pizzazz, int[] preferences, int[] scores) {
        ID = id;
        HandEye = handEye;
        Endurance = endurance;
        Pizzazz = pizzazz;
        Preferences = preferences;
        Scores = scores;
        isMatched = false;
        CurrentCircuit = NONE;
        CurrentScore = NONE;
        Index = NONE;
    }

    // these methods are all self explanatory and they are similar to the circuit methods
    public boolean isMatched() {
        return isMatched;
    }

    
    public int getNumber() {
        return ID;
    }

    
    public int getEndurance() {
        return Endurance;
    }

    
    public int getHandEye() {
        return HandEye;
    }

    
    public int getPizzazz() {
        return Pizzazz;
    }

    
    public int[] getScores() {
        return Scores;
    }

    
    public int[] getPreferences() {
        return Preferences;
    }

    
    public int getCurrentCircuit() {
        return CurrentCircuit;
    }

    
    public int getCurrentScore() {
        return CurrentScore;
    }

    
    public int getIndex() {
        return Index;
    }

    
    public int getID() {
        return ID;
    }

    
    public void setMatched(boolean b) {
        isMatched = b;
    }

    
    public void setCurrentCircuit(int circuitID) {
        CurrentCircuit = circuitID;
    }

    
    public void setCurrentScore(int score) {
        CurrentScore = score;
    }

    
    void setIndex(int index) {
        Index = index;
    }

    public int compareTo(Object o) {
        Juggler j = (Juggler) o;
        if (CurrentScore < j.getCurrentScore()) {
            return 1;
        } else if (CurrentScore == j.getCurrentScore()) {
            return 0;
        } else {
            return -1;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("J").append(ID);

        for (int i = 0; i < Preferences.length; ++i) {
            sb.append(" C").append(Preferences[i]).append(":").append(Scores[i]);
        }

        return sb.toString();
    }
}

/**
 * Circuit description given by the problem
 */
class Circuit {

    private int ID;
    private int HandEye;
    private int Endurance;
    private int Pizzazz;
    private ArrayList<Juggler> Jugglers;
    //private int jugglersSize;
    private int MinScore;

    /**
     * creates the circuit object
     */
    public Circuit(int id, int handEye, int endurance, int pizzazz) {
        ID = id;
        HandEye = handEye;
        Endurance = endurance;
        Pizzazz = pizzazz;
        Jugglers = new ArrayList<>();
        MinScore = Integer.MAX_VALUE;
        //jugglersSize = myJugglers.size(); //for optimization
    }

    
    public int getID() {
        return ID;
    }

    
    public int getHandEye() {
        return HandEye;
    }

    
    public int getEndurance() {
        return Endurance;
    }

    
    public int getPizzazz() {
        return Pizzazz;
    }

    
    public int getMinScore() {
        return MinScore;
    }

    
    public ArrayList<Juggler> getJugglers() {
        return Jugglers;
    }

    
    public void addJuggler(Juggler newJuggler) {
        Jugglers.add(newJuggler);
        sortTheJugglers();
        MinScore = Jugglers.get(Jugglers.size() - 1).getCurrentScore();
    }

    
    public Juggler replaceWeakestJuggler(Juggler newJuggler) {

        Juggler removedJuggler = Jugglers.remove(Jugglers.size() - 1);
        removedJuggler.setMatched(false);
        newJuggler.setMatched(true);
        addJuggler(newJuggler);

        return removedJuggler;
    }

    
    private void sortTheJugglers() {
    	
        Collections.sort(Jugglers);
    
    }
    

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("C").append(ID).append(" ");

        for (int i = 0; i < Jugglers.size() - 1; i++) {
            builder.append(Jugglers.get(i)).append(",");
        }
        builder.append(Jugglers.get(Jugglers.size() - 1));

        return builder.toString();
    }
}
