/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.clustering;

import agent.RVOAgent;
import environment.RVOSpace;
import java.util.ArrayList;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import utility.Geometry;

/**
 *
 * @author Vaisagh
 */
public class ClusteredAgent extends RVOAgent {

    public static double MAXCLUSTERRADIUS = 0.8f;
    protected ArrayList<RVOAgent> agents;
    protected double maxRadius = MAXCLUSTERRADIUS;
  
    public ClusteredAgent(RVOSpace rvoSpace, RVOAgent agent) {
        super(rvoSpace);
        this.radius = agent.getRadius();
        this.velocity = new Vector2d(agent.getVelocity());
        this.setCentre(new Point2d(agent.getCurrentPosition()));
        agents = new ArrayList<RVOAgent>();
        agents.add(agent);

    }

    @Override
    public double getRadius() {
        return this.radius;
    }

//    public double getActualRadius() {
//        return radius;
//    }
    @Override
    public Vector2d getVelocity() {
        return velocity;
    }

    public Point2d getCentre() {
        return currentPosition;
    }

    public ArrayList<RVOAgent> getAgents() {
        return agents;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public void setVelocity(Vector2d velocity) {
        this.velocity = new Vector2d(velocity);
    }

    public void setCentre(Point2d centre) {
        this.currentPosition = new Point2d(centre);
    }

    /**
     * This function tries to add the agent and returns a false if it isn't added
     * to the current cluster.
     *
     *
     * @param agent
     * @return
     */
    public boolean addAgent(RVOAgent agent) {

        //If the agent velocity is in opposite direction.Then don't add it to cluster
//        if (Geometry.sameDirection(agent.getVelocity(), this.getVelocity()) < 0.0) {
//            if (Math.abs(agent.getVelocity().length() - this.getVelocity().length()) < 0.1) {
//                return false;
//            }
//        }


        Vector2d differenceVelocity = new Vector2d(agent.getVelocity());
        differenceVelocity.sub(this.getVelocity());
        if (Math.abs(differenceVelocity.length()) > 0.5) {

            return false;
        }




//        System.out.println("****************\nAdding agent "+agent.getId() +" to cluster");
//        System.out.println("Current Cluster center"+ this.getCentre());
//        System.out.println("New point position "+ agent.getCurrentPosition());

        double distanceFromCentreToAgent = agent.getCurrentPosition().distance(this.getCentre());
//        System.out.println("Distance from center to new position"+distanceFromCentreToAgent );
        if (distanceFromCentreToAgent < this.getRadius()) {

            double newCentreX = ((this.getCentre().x * this.agents.size()) + agent.getX()) / (agents.size() + 1);
            double newCentreY = ((this.getCentre().y * this.agents.size()) + agent.getY()) / (agents.size() + 1);
            this.setCentre(new Point2d(newCentreX, newCentreY));

            velocity.scale(agents.size() - 1);
            velocity.add(agent.getVelocity());
            velocity.scale(1.0 / agents.size());

            double maxDistance = Double.MIN_VALUE;

            for (RVOAgent tempAgent : agents) {





                Vector2d distance = new Vector2d(tempAgent.getCurrentPosition());
                distance.sub(this.getCentre());
                if (distance.length() > maxDistance) {
                    maxDistance = distance.length();
                }
            }
            this.setRadius(maxDistance + RVOAgent.RADIUS);

            agents.add(agent);


//            System.out.println("Within cluster oledi\n***************");
            return true;
        } else if ((distanceFromCentreToAgent + this.getRadius()) < (2.0 * maxRadius)) {
//            System.out.println("Can be fitted in this cluster");
//            Vector2d agentToCenter = new Vector2d(this.getCentre());
//            agentToCenter.sub(new Vector2d(agent.getCurrentPosition()));
//            agentToCenter.normalize();
//            agentToCenter.scale(-(distanceFromCentreToAgent) / 2.0);
//            agentToCenter.add(this.getCentre());
//            System.out.println("New center : "+agentToCenter);
//            System.out.println("Old radius : "+this.getRadius());
//            this.setCentre(new Point2d(agentToCenter));
//            this.setRadius((distanceFromCentreToAgent + (2.0 * this.getRadius())) / 2.0);
            //        System.out.println("New radius"+this.getRadius()+"\n****");

            /**
             *new velocity  = old velocity *(n-1) + new agent velocity / n
             */
            double newCentreX = ((this.getCentre().x * this.agents.size()) + agent.getX()) / (agents.size() + 1);
            double newCentreY = ((this.getCentre().y * this.agents.size()) + agent.getY()) / (agents.size() + 1);
            this.setCentre(new Point2d(newCentreX, newCentreY));



            velocity.scale(agents.size() - 1);
            velocity.add(agent.getVelocity());
            velocity.scale(1.0 / agents.size());


            agents.add(new RVOAgent(agent));

            double maxDistance = Double.MIN_VALUE;
            for (RVOAgent tempAgent : agents) {
                Vector2d distance = new Vector2d(tempAgent.getCurrentPosition());
                distance.sub(this.getCentre());
                if (distance.length() > maxDistance) {
                    maxDistance = distance.length();
                }
            }
            this.setRadius(maxDistance + RVOAgent.RADIUS);
            return true;
        }
        return false;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(double maxRadius) {
        this.maxRadius = maxRadius;
    }
}
