package app;

import agent.AgentGenerator;
import agent.RVOAgent;
import agent.clustering.ClusteredSpace;
import environment.Obstacle.RVOObstacle;
import environment.RVOSpace;
import environment.XMLManager;
import environment.geography.Agent;
import environment.geography.AgentLine;
import environment.geography.Goals;
import environment.geography.Obstacle;
import environment.geography.SimulationScenario;
import agent.latticegas.LatticeSpace;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point2d;
import javax.xml.bind.JAXBException;
import sim.engine.SimState;

/**
 * RVOModel
 *
 * @author michaellees
 * Created: Nov 24, 2010
 *
 * Copyright michaellees
 *
 * Description:
 *
 * The RVOModel is the core of the program. It contains the agentList,
 * the obstacleList and the rvoSpace and also the different parameters of the
 * MODEL itself. It also contains the main().
 *
 */
public class RVOModel extends SimState {

    public static enum Model {
        RVO2, PatternBasedMotion, RVO1Standard, RVO1Acceleration, RuleBasedNew
    }
    
    
    public static final long SEED = 10;
    private static final int WORLDXSIZE = 10;
    private static final int WORLDYSIZE = 10;
    private static final float GRIDSIZE = 1.0f;
    public final static double TIMESTEP = 0.25f;
    public static final Model MODEL = Model.RVO2;
    public static final boolean LATTICEMODEL = false;
    public static final boolean INFOPROCESSING = false;
    public static final boolean USECLUSTERING = false;
    public static final boolean INITIALISEFROMXML = true;
    public static final String xmlSourceFolder ="xml-resources\\scenarios";
    public static final String FILEPATH = xmlSourceFolder +"\\EvacTest\\4.xml";
    private int worldXSize = WORLDXSIZE;
    private int worldYSize = WORLDYSIZE;
    private float gridSize = GRIDSIZE;
    private RVOSpace rvoSpace;
    private LatticeSpace latticeSpace;
    /**
     * Actually initialised to ArrayList as can be seen in second constructor, 
     * But using List type here so that we can change the implementation later 
     * if needed
     */
    private List<RVOAgent> agentList;
    private List<RVOObstacle> obstacleList;
    private List<AgentGenerator> agentLineList;

//    //the list to keep record of every agent's status in each timestemp
//    //each record contains a list of status for each agent
//    //for each agent, there is a list to record all the necessary status (e.g., velocity, position etc)
//    public ArrayList<ArrayList<ArrayList>> records;
    public RVOModel() {
        this(RVOModel.SEED);

    }

    public RVOModel(long seed) {
        super(seed);

        if (INITIALISEFROMXML) {

            try {
                XMLManager settings = XMLManager.instance();
                SimulationScenario scenario = (SimulationScenario) settings.unmarshal(FILEPATH);
                RVOGui.scale = scenario.getScale();
                worldXSize = RVOGui.checkSizeX = scenario.getXsize();
                worldYSize = RVOGui.checkSizeY = scenario.getYsize();


            } catch (JAXBException ex) {
                Logger.getLogger(RVOModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }



        agentList = new ArrayList<RVOAgent>();
        obstacleList = new ArrayList<RVOObstacle>();
        agentLineList = new ArrayList<AgentGenerator>();


    }

    @Override
    public void start() {

        super.start();

        // This function is equivalent to a reset. 
        //Need to readup a bit more to see if it is even necessary...
        setup();

        if (INITIALISEFROMXML) {
            initialiseFromXML();
        } else {
            buildSpace();
            createAgents();
        }
    }

    /**
     * Creates the continuous 2D space that will be used for the simulation. 
     * Creates an appropriate space for Clustering
     */
    private void buildSpace() {

        if (!RVOModel.USECLUSTERING) {
            rvoSpace = new RVOSpace(worldXSize, worldYSize, gridSize, this);
        } else {
            rvoSpace = new ClusteredSpace(worldXSize, worldYSize, gridSize, this);

        }

        // rvoSpace.initialiseGeography();

    }

    /**
     * To use RVO without using an xml to initialise layout. Arranges agents in 
     * a pre decided order
     */
    private void createAgents() {
        int numAgentsPerSide = 5;
        double gap = 1.5f;
        for (int i = 1; i < numAgentsPerSide + 1; i++) {
            addNewAgent(
                    new RVOAgent(new Point2d(2, i * gap + 0.5),
                    new Point2d(8, i * gap + 0.5),
                    this.rvoSpace,
                    new Color(Color.HSBtoRGB((float) i / (float) numAgentsPerSide, 1.0f, 0.68f))));
            addNewAgent(
                    new RVOAgent(new Point2d(8, i * gap + 0.5),
                    new Point2d(2, i * gap + 0.5),
                    rvoSpace,
                    new Color(Color.HSBtoRGB(0.7f - (float) i / (float) numAgentsPerSide, 1.0f, 0.68f))));
        }
//        this.addNewAgent(new RVOAgent( new Point2d(8,3.5), new Point2d(2,3.5)
//        , this.rvoSpace, new Color(Color.HSBtoRGB(1.0f, 1.0f, 0.68f))));

    }

    /**
     * resets all the values to the initial values. this is just to be safe.
     */
    public void setup() {

        rvoSpace = null;
        agentList = new ArrayList<RVOAgent>();
        obstacleList = new ArrayList<RVOObstacle>();
        agentLineList = new ArrayList<AgentGenerator>();
        RVOAgent.agentCount = 0;

    }

    public List<RVOAgent> getAgentList() {
        return agentList;
    }

    public static double getTimeStep() {
        return TIMESTEP;
    }

    public int getWorldYSize() {
        return worldYSize;
    }

    public int getWorldXSize() {
        return worldXSize;
    }

    public float getGridSize() {
        return gridSize;
    }

    public RVOSpace getRvoSpace() {
        return rvoSpace;
    }

    LatticeSpace getLatticeSpace() {
        return latticeSpace;
    }

    private void addNewObstacle(RVOObstacle obstacle) {
        obstacleList.add(obstacle);
        rvoSpace.addNewObstacle(obstacle);
    }

    public void addNewAgent(RVOAgent a) {
        a.scheduleAgent();
        agentList.add(a);
        rvoSpace.updatePositionOnMap(a, a.getX(), a.getY());
        if (RVOModel.LATTICEMODEL) {
            latticeSpace.addAgentAt(a.getX(), a.getY());
        }
    }

    private void addNewAgentLine(AgentGenerator tempAgentLine, int frequency) {
        agentLineList.add(tempAgentLine);
        schedule.scheduleRepeating(tempAgentLine, 1, (double) frequency);

    }

    public static void main(String[] args) {
        // Read tutorial 2 of mason to see what this does.. or refer to documentation of this function
        doLoop(RVOModel.class, args);
        System.exit(0);
    }

    
    /**
     * Initialize data from the XML file specified
     */
    private void initialiseFromXML() {
        try {
            XMLManager settings = XMLManager.instance();
            SimulationScenario scenario = (SimulationScenario) settings.unmarshal(FILEPATH);
            System.out.println("Running buildModel()");

            if (!RVOModel.USECLUSTERING) {
                rvoSpace = new RVOSpace(worldXSize, worldYSize, gridSize, this);
            } else {
                rvoSpace = new ClusteredSpace(worldXSize, worldYSize, gridSize, this);
                ((ClusteredSpace) rvoSpace).scheduleClustering();
            }

            if (RVOModel.LATTICEMODEL) {
                latticeSpace = new LatticeSpace(worldXSize, worldYSize, this);
                latticeSpace.scheduleLattice();
                latticeSpace.setDirection(scenario.getDirection());

                List<Goals> xmlGoalList = scenario.getEnvironmentGoals();
                for (int i = 0; i < xmlGoalList.size(); i++) {
                    Goals tempGoal = xmlGoalList.get(i);
                    latticeSpace.addGoal(tempGoal);
                }
            }


            List<Agent> xmlAgentList = scenario.getCrowd();
            for (int i = 0; i < xmlAgentList.size(); i++) {
                Agent tempAgent = xmlAgentList.get(i);
                RVOAgent tempRVOAgent = new RVOAgent(
                        new Point2d(tempAgent.getPosition().getX(), tempAgent.getPosition().getY()),
                        new Point2d(tempAgent.getGoal().getX(), tempAgent.getGoal().getY()),
                        rvoSpace,
                        new Color(Color.HSBtoRGB((float) i / (float) xmlAgentList.size(),
                        1.0f, 0.68f)));
                tempRVOAgent.setPreferredSpeed(tempAgent.getPreferedSpeed());
                tempRVOAgent.setCommitmentLevel(tempAgent.getCommitmentLevel());
                addNewAgent(tempRVOAgent);
            }


            List<Obstacle> xmlObstacleList = scenario.getObstacles();
            for (int i = 0; i < xmlObstacleList.size(); i++) {
                Obstacle tempObst = xmlObstacleList.get(i);
                RVOObstacle tempRvoObst = new RVOObstacle(tempObst);
                addNewObstacle(tempRvoObst);
                if (RVOModel.LATTICEMODEL) {
                    latticeSpace.addObstacle(tempObst);
                }
            }

            List<AgentLine> xmlAgentLineList = scenario.getGenerationLines();
            for (int i = 0; i < xmlAgentLineList.size(); i++) {

                Point2d start = new Point2d(xmlAgentLineList.get(i).getStartPoint().getX(), xmlAgentLineList.get(i).getStartPoint().getY());
                Point2d end = new Point2d(xmlAgentLineList.get(i).getEndPoint().getX(), xmlAgentLineList.get(i).getEndPoint().getY());
                AgentGenerator tempAgentLine = new AgentGenerator(start, end, scenario.getGenerationLines().get(i).getNumber(), scenario.getGenerationLines().get(i).getDirection(), scenario.getEnvironmentGoals(), this);
                addNewAgentLine(tempAgentLine, scenario.getGenerationLines().get(i).getFrequency());
            }

        } catch (JAXBException ex) {
            Logger.getLogger(RVOModel.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
