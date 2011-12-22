package motionPlanners.socialforce;

import agent.RVOAgent;
import app.PropertySet;
import environment.geography.Obstacle;
import environment.geography.Position;
import javax.vecmath.Vector2d;
import motionPlanners.VelocityCalculator;
import sim.util.Bag;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author steven
 */
public class SocialForce implements VelocityCalculator {

    public static int worldXSize;
    public static int worldYSize;
    
    static double[][] Fw;
    static double[][] Fwx;
    static double[][] Fwy;
    static int N0x; //PropertySet.WORLDXSIZE;        // Total number of cell in X direction
    static int N0y; //PropertySet.WORLDYSIZE;        // Total number of cell in Y direction
    static double Xmin = 0;                              // Minimum x point of terrain
    static double Xmax = 30;         // Maximum x point of terrain (+5 is for buffer zone)
    static double Ymin = 0;                              // Minimum y point of terrain
    static double Ymax = 30;         // Maximum y point of terrain

    // /////////////////////////////////////////////////////////////////////////
    // Psudeo function
    // /////////////////////////////////////////////////////////////////////////
    private double g(double x) {
        if (x >= 0)  
        {
            return x;
        } else {
            return 0;
        }
    }

    
    // /////////////////////////////////////////////////////////////////////////
    // Bound the force so that agent don't "explode" (unit = Newton)
    // /////////////////////////////////////////////////////////////////////////
    private double forceBound(double x) {
        double var = 1.5;

        if (x > var) {
            x = Math.random();
            //x = var;
        } else if (x < -var) {
            x = -(Math.random());
            //x = -var;
        }

        return x;
    }

    
    // /////////////////////////////////////////////////////////////////////////
    // MATLAB linspace function
    // /////////////////////////////////////////////////////////////////////////
    private static double[] linearSpaceVector(double a, double b, int c){
        // Create a vector of c elements starting with a, interval difference
        // of intervalValue until b.
        double intervalValue = (b-a)/(c-1);
        double[] resultVector = new double[c];
        
        resultVector[0] = a;
        
        for (int i=0; i<c-1; i++){
            resultVector[i+1] += resultVector[i]+intervalValue;
        }
        resultVector[c-1]=b;
        
        return resultVector;
    } 
    
    
    // /////////////////////////////////////////////////////////////////////////
    // Average function
    // /////////////////////////////////////////////////////////////////////////
    private static double average(double[] a) { 
        double result = 0;
        int count = 0;
        
        for(int i=0; i < a.length; i++){
          result = result + a[i];
          count+=1;
        }
        
        return result/ count;
    }
    
    
    // /////////////////////////////////////////////////////////////////////////
    // Average over surrounding (8 cell) forces
    // /////////////////////////////////////////////////////////////////////////
    private static double[] averageSurroundForce(double Pxi, double Pyi, double Xmax, double Ymax, int N0x, int N0y, double[][] F, double[][] Fx, double[][] Fy) {
//        System.out.println("(" + Pxi + ", " + Pyi + ")");
//        System.out.println("(" + (Pxi/Xmax)*(N0x) + ", " + (Pyi/Ymax)*(N0y) + ")");
        double bcellix = (Pxi/Xmax)*(N0x);    // X cell number of agent
        double bcelliy = (Pyi/Ymax)*(N0y);    // Y cell number of agent
        int cellix = (int) bcellix;
        int celliy = (int) bcelliy;
//        System.out.println("(" + cellix + ", " + celliy + ")");
        
        
        int[] neighbouricelly;
        int[] neighbouricellx;
        
        // Check cell boundary condition
        if (cellix==0 && celliy==0){   // Left Top corner
//            System.out.println("Case Top Left Corner; ");
            neighbouricellx = new int[] {0,1,1};    //{1,2,2};
            neighbouricelly = new int[] {1,0,1};    //{2,1,2};
        }
        else if (cellix==0 && celliy== N0y-1){     // Right top corner
//            System.out.println("Case Top Right Corner; ");
            neighbouricellx = new int[] {0,1,1};                //{1,2,2};
            neighbouricelly = new int[] {N0y-2,N0y-2,N0y-1};    //{N0y-1,N0y-1,N0y};
        }
        else if (cellix==N0x-1 && celliy== 0){     // Left bottom corner
//            System.out.println("Case Bottom Left Corner; ");
            neighbouricellx = new int[] {N0x-2,N0x-1,N0x-2};  //{N0x-1,N0x,N0x-1};
            neighbouricelly = new int[] {0,1,1};    //{1,2,2};
        }
        else if (cellix==N0x-1 && celliy== N0y-1){    // Right bottom corner
//            System.out.println("Case Bottom Right Corner; ");
            neighbouricellx = new int[] {N0x-1,N0x-2,N0x-1};        //{N0x,N0x-1,N0x};
            neighbouricelly = new int[] {N0y-2,N0y-2,N0y-2};    //{N0y-1,N0y-1,N0y-1};
        }
        else if (cellix==0){    // Top row all column
//            System.out.println("Case Top Row; ");
            neighbouricellx = new int[] {0,0,1,1,1};    //{1,1,2,2,2};
            neighbouricelly = new int[] {celliy-2,celliy,celliy-2,celliy-1,celliy};   //{celliy-1,celliy+1,celliy-1,celliy,celliy+1}
        }
        else if (celliy==0){    // Left column all row
//            System.out.println("Case Left Column; ");
            neighbouricellx = new int[] {cellix-2,cellix,cellix-2,cellix-1,cellix};   //{cellix-1,cellix+1,cellix-1,cellix,cellix+1};
            neighbouricelly = new int[] {0,0,1,1,1};    //{1,1,2,2,2};
        }
        else if (celliy==N0y-1){   // Right column all row
//            System.out.println("Case Right Column; ");
            neighbouricellx = new int[] {cellix-2,cellix,cellix-2,cellix-1,cellix};   //{cellix-1,cellix+1,cellix-1,cellix,cellix+1};
            neighbouricelly = new int[] {N0y-1,N0y-1,N0y-2,N0y-2,N0y-2};        //{N0y,N0y,N0y-1,N0y-1,N0y-1};
        }
        else if (cellix==N0x-1){   // Bottom row all column
//            System.out.println("Case Bottom Row; ");
            neighbouricellx = new int[] {N0x-1,N0x-1,N0x-2,N0x-2,N0x-2};    //{N0x,N0x,N0x-1,N0x-1,N0x-1};
            neighbouricelly = new int[] {celliy-2,celliy,celliy-2,celliy-1,celliy};   //{celliy-1,celliy+1,celliy-1,celliy,celliy+1};
        }
        else{   // Otherwise
//            System.out.println("Case Normal; ");
            neighbouricellx = new int[] {cellix-1,cellix-1,cellix,cellix,cellix,cellix-2,cellix-2,cellix-2};  //{cellix,cellix,cellix+1,cellix+1,cellix+1,cellix-1,cellix-1,cellix-1};
            neighbouricelly = new int[] {celliy-2,celliy,celliy-2,celliy-1,celliy,celliy-2,celliy-1,celliy};  //{celliy-1,celliy+1,celliy-1,celliy,celliy+1,celliy-1,celliy,celliy+1};
        }
        
//        for (int i=0; i<neighbouricellx.length; i++){
//            System.out.print(neighbouricellx[i] + " ");
//        }
        
        // Collecting neighbour Force magnitude and direction into an array
        int Nneighbour = neighbouricellx.length;
        double[] FAvg = new double[Nneighbour];   // Force magnitude
        double[] FxAvg = new double[Nneighbour];  // Force direction
        double[] FyAvg = new double[Nneighbour];
        for (int i = 0; i<Nneighbour; i++){
            //System.out.print("(" + neighbouricellx[i] + ", " + neighbouricelly[i] + ") ");
            // Assigning value into buffer
            FAvg[i] = F[neighbouricelly[i]][neighbouricellx[i]];
            FxAvg[i] = Fx[neighbouricelly[i]][neighbouricellx[i]];
            FyAvg[i] = Fy[neighbouricelly[i]][neighbouricellx[i]];
            
        }
        FAvg[Nneighbour-1] = F[celliy][cellix];
        FxAvg[Nneighbour-1] = Fx[celliy][cellix];
        FyAvg[Nneighbour-1] = Fy[celliy][cellix];
        
//        for (int i=0; i<Nneighbour; i++)
//            System.out.print(FAvg[i] + " ");
//        System.out.println();
        
        // Average over force
        double meanFAvg = average(FAvg);
        //System.out.println(meanFAvg);
        double meanFxAvg = average(FxAvg);
        double meanFyAvg = average(FyAvg);
//        System.out.println(meanFxAvg + " " + meanFyAvg);
        
        double Fxi = -meanFxAvg*meanFAvg;
        double Fyi = -meanFyAvg*meanFAvg;

        return new double[]{Fxi,Fyi};
    }
    
    
    // /////////////////////////////////////////////////////////////////////////
    // Pre-calculate wall force matrix
    // /////////////////////////////////////////////////////////////////////////
    private static double[][][] PreCalcForce(int N0x, int N0y, double[] vertexx, double[] vertexy, double Xmin, double Xmax, double Ymin, double Ymax){
            
            double[] x = linearSpaceVector(Xmin,Xmax,N0x);
            double[] y = linearSpaceVector(Ymin,Ymax,N0y);

            // Exponential force constant
            double A_wall = 100000;             //10  Diameter of the wall
            double B_wall = 50;             //10  Steepness of the wall
            double A_corner = A_wall;      //5 Diameter of the pole
            double B_corner = B_wall;	//5 Steepness of the pole

            // Initialize every cell of force
            double[][] F = new double[N0y][N0x];
            double[][] Fx = new double[N0y][N0x];
            double[][] Fy = new double[N0y][N0x];
            for (int i=0; i<N0x; i++){
                for (int j=0; j<N0y; j++){
                    F[j][i] = 0;
                    Fx[j][i] = 0;
                    Fy[j][i] = 0;
                }
            }

            int Nvertex = vertexx.length;  // total number of vertex 

            for (int p=0; p<Nvertex-1; p++){
                // Vector point from vertex p+1 to p
                double bx = vertexx[p+1]-vertexx[p];
                double by = vertexy[p+1]-vertexy[p];
                double normb = Math.sqrt(bx*bx+by*by);
                double bxhat = bx/normb;
                double byhat = by/normb;

                for (int i=0; i<N0x; i++){
                    for (int j=0; j<N0y; j++){

                        ////////////////////////////////////////////////////////////
                        // Wall Potential //////////////////////////////////////////
                        ////////////////////////////////////////////////////////////
                        double a = (x[i]-vertexx[p])*bxhat+(y[j]-vertexy[p])*byhat;
                        if (a>=0 && a<=normb){
                            // Vector points on wall's line
                            double wx = a*bxhat + vertexx[p];
                            double wy = a*byhat + vertexy[p];
                            //double normw = Math.sqrt(wx*wx+wy*wy);

                            // Vector point perpendicular to wall's line
                            double rx = x[i]-wx;
                            double ry = y[j]-wy;
                            double normr = Math.sqrt(rx*rx+ry*ry);

                            // Speed magnitude (Exponential potential)
                            double F0 = A_wall*Math.exp(-B_wall*normr);
                            F[j][i] += F0;
                        }

                        ////////////////////////////////////////////////////////////
                        // Half pole at 1st (p) end of wall ////////////////////////
                        ////////////////////////////////////////////////////////////
                        double rx = x[i]-vertexx[p];
                        double ry = y[j]-vertexy[p];
                        double normr = Math.sqrt(rx*rx+ry*ry);

                        double ax = -bx;
                        double ay = -by;
                        double norma = Math.sqrt(ax*ax+ay*ay);

                        // Only calc for half a circle
                        if ((rx*ax+ry*ay)/(normr*norma)>Math.cos(Math.PI/2)){
                            // Speed magnitude (Exponential potential)
                            F[j][i] += A_corner*Math.exp(-B_corner*normr);
                        }

                        ////////////////////////////////////////////////////////////
                        // Half pole at 2nd (p+1) end of wall //////////////////////
                        ////////////////////////////////////////////////////////////
                        rx = x[i]-vertexx[p+1];
                        ry = y[j]-vertexy[p+1];
                        normr = Math.sqrt(rx*rx+ry*ry);

                        ax = bx;
                        ay = by;
                        norma = Math.sqrt(ax*ax+ay*ay);

                        if ((rx*ax+ry*ay)/(normr*norma)>Math.cos(Math.PI/2)){
                            // Speed magnitude (Exponential potential)
                            F[j][i] += A_corner*Math.exp(-B_corner*normr);
                        }

                        ////////////////////////////////////////////////////////////
                        // Pole deletion at intersection corner ////////////////////
                        ////////////////////////////////////////////////////////////
                        if ((p>1 && p<Nvertex) || (vertexx[1]==vertexx[Nvertex-1] && vertexy[1]==vertexy[Nvertex-1]) ){
                            rx = x[i]-vertexx[p];
                            ry = y[j]-vertexy[p];
                            normr = Math.sqrt(rx*rx+ry*ry);

                            // Speed magnitude (Exponential potential)
                            F[j][i] -= A_corner*Math.exp(-B_corner*normr);
                            
                        }
                        
                    }//end for j loop
                }// end for i loop
            }//end for p loop

            ////////////////////////////////////////////////////////////////////////
            // Force field direction 
            ////////////////////////////////////////////////////////////////////////
            double deltay = y[2] - y[1];
            for (int i=0; i<N0x; i++){
                // First row
                Fy[0][i]=(F[1][i]-F[0][i])/deltay;
                // Last row
                Fy[N0y-1][i]=(F[N0y-1][i]-F[N0y-2][i])/deltay;
                // Middle row
                for (int j=1; j<=N0y-2; j++){
                    Fy[j][i]=(F[j+1][i]-F[j-1][i])/deltay;
                }
            }

            double deltax = x[2] - x[1];
            for (int j=0; j<N0y; j++){
                // First column
                Fx[j][0]=(F[j][1]-F[j][0])/deltax;
                // Last column
                Fx[j][N0x-1]=(F[j][N0x-1]-F[j][N0x-2])/deltax;
                // Middle row
                for (int i=1; i<=N0x-2; i++){
                    // CHCECK WHETHER IS 2 deltax or deltax!!!!!!!
                    Fx[j][i]=(F[j][i+1]-F[j][i-1])/deltax;
                }
            }

            // Dot product of the force field matrix
            for (int i=0; i<N0x; i++){
                for (int j=0; j<N0y; j++){
                    Fx[j][i] = Fx[j][i]*F[j][i];
                    Fy[j][i] = Fy[j][i]*F[j][i];
                }
            }
            
            // Prepare to output the 3 2D matrix
            double[][][] result = new double[3][][];
            result[0] = F;
            result[1] = Fx;
            result[2] = Fy;
            
//            for (int j=0; j<N0y; j++){
//                for (int i=0; i<N0x; i++){
//                    System.out.print(Fx[i][j] + " ");
//                }
//                System.out.println();
//            } 
            
            return result;
    }
    
    
    // /////////////////////////////////////////////////////////////////////////
    // Building the wall force matrix
    // /////////////////////////////////////////////////////////////////////////
    public static void initializeObstacleSet(List<Obstacle> xmlObstacleList) {
        /**
         * By the end of this function set of vertices and static forces will be created.
         */
        // For every building
        
        N0x = 20*worldXSize; //PropertySet.WORLDXSIZE;        // Total number of cell in X direction
        N0y = 20*worldYSize; //PropertySet.WORLDYSIZE;        // Total number of cell in Y direction
    
        
        for (Obstacle building : xmlObstacleList) {
            ArrayList<Position> vertices = (ArrayList<Position>) building.getVertices();
            double vertexX[] = new double[vertices.size()];
            double vertexY[] = new double[vertices.size()];
            
            // For every vertices
            for (int i = 0; i < vertices.size(); i++) {
                vertexX[i] = vertices.get(i).getX();
                vertexY[i] = vertices.get(i).getY();    // Vertex coordinate y
            }
            
            // #####################################################################
            // PRECALCULATE WALL FORCE GRID
//            // #####################################################################
//            int N0x = 10 * PropertySet.WORLDXSIZE;        // Total number of cell in X direction
////        System.out.println(PropertySet.WORLDXSIZE);
//        int N0y = 10 * PropertySet.WORLDYSIZE;        // Total number of cell in Y direction
//        double Xmin = -10;                              // Minimum x point of terrain
//        double Xmax = PropertySet.WORLDXSIZE+5;         // Maximum x point of terrain (+5 is for buffer zone)
//        double Ymin = -10;                              // Minimum y point of terrain
//        double Ymax = PropertySet.WORLDYSIZE+5;         // Maximum y point of terrain

            double[][][] preFw = PreCalcForce(N0x, N0y, vertexX, vertexY, Xmin, Xmax, Ymin, Ymax);
            Fw = preFw[0];
            Fwx = preFw[1];
            Fwy = preFw[2];
            
        }
        
        // for multiple building Fw, Fwx and Fwy must be accumulated!
        
        
    }
    

    // /////////////////////////////////////////////////////////////////////////
    // Update velocity
    // /////////////////////////////////////////////////////////////////////////
    @Override
    public Vector2d calculateVelocity(RVOAgent me, Bag neighbors, Bag obses,
            Vector2d preferredVelocity, double timeStep) {
        
        // #####################################################################
        // INITIALIZE VARIABLES
        // #####################################################################
        // Constant for agent-agent interaction
        final int Aj = 2000;       //Published value = 2000 N
        final double Bj = 0.08;    //Published value = 0.08 m
        final int kj = 12000;      //Published value = 120000 kg/s^2
        final int kappaj = 24000;  //Published value = 240000 kg/ms

        // Constant for agent-wall interaction
        //final int Aw = 5000;       //
        //final double Bw = 0.1;    //
        //final int kw = 12000;      //
        //final int kappaw = 24000;  //

        //final double Rw = 0.3;  // Imaginary wall radius

        double Pxi = me.getCurrentPosition().getX();    // X-position
        double Pyi = me.getCurrentPosition().getY();    // Y-position
        double Vxi = me.getVelocity().getX();           // X-velocity
        double Vyi = me.getVelocity().getY();           // Y-velocity
        double Ri = me.getRadius();                     // Agent's radius
        double Mi = me.getMass();                       // Agent's mass
        double fijx = 0;    //  X-direction interaction force initialization
        double fijy = 0;    //  Y-direction interaction force initialization

        // #####################################################################
        // Update everytime step for every neighbour
        // #####################################################################
        for (int i = 0; i < neighbors.size(); i++) {

            RVOAgent tempAgent = (RVOAgent) neighbors.get(i);

            if (tempAgent.equals(me)) {
                continue;   // skip agent i itself for the calculation
            }
//            else if (tempAgent.isSocialForceObstacle()==true){
//                continue;
//            }
            //If this is real agent
            double Pxj = tempAgent.getCurrentPosition().getX();
            double Pyj = tempAgent.getCurrentPosition().getY();
            double Vxj = tempAgent.getVelocity().getX();
            double Vyj = tempAgent.getVelocity().getY();
            double Rj = tempAgent.getRadius();

            double Rij = Ri + Rj;   //System.out.println("Rij: "+Rij);
            double Dij = Math.sqrt((Pxi - Pxj) * (Pxi - Pxj) + (Pyi - Pyj) * (Pyi - Pyj));
//                if (Dij < 0.01) {
//                    System.out.println("ERR! Dij<0.01. Dij " + Dij + " Pxi: " + Pxi + " Pxj: " + Pxj);
//                    Dij = 1;
//                }

            double RD = Rij - Dij;
            double Nijx = (Pxi - Pxj) / Dij;    // Normal X-direction unit vector
            double Nijy = (Pyi - Pyj) / Dij;    // Normal Y-direction unit vector
            double Tijx = -Nijy;            // Tangential X-direction unit vector
            double Tijy = Nijx;             // Tangential Y-direction unit vector
            double DeltaVtji = (Vxj - Vxi) * Tijx + (Vyj - Vyi) * Tijy;

            // Social Repulsion force
            double fsr = Aj * Math.exp(RD / Bj);
            // Pushing force
            double fp = kj * g(RD);
            // Friction force
            double ff = kappaj * g(RD) * DeltaVtji;

            //System.out.println(fsr + " " + fp + " " + ff);

            fijx += (fsr + fp) * Nijx + ff * Tijx;
            fijy += (fsr + fp) * Nijy + ff * Tijy;
        }

        double Vxint = (fijx) / Mi * timeStep;
        double Vyint = (fijy) / Mi * timeStep;

//         #################################################################
//         BOUNDARY FORCE
//         #################################################################
//        double fiwx = 0;    //  X-direction interaction force initialization
//        double fiwy = 0;    //  Y-direction interaction force initialization
//
//        for (int i = 0; i < neighbors.size(); i++) {
//            RVOAgent tempObstacle = (RVOAgent) neighbors.get(i);
//            
//            if (tempObstacle.equals(me)){
//                continue;   // skip agent i itself for the calculation
//            }
////            else if (tempObstacle.isSocialForceObstacle()==false){
////                continue;
////            }
//
//            double Wx = tempObstacle.getX();
//            double Wy = tempObstacle.getY();
//            double Diw = Math.sqrt((Pxi - Wx) * (Pxi - Wx) + (Pyi - Wy) * (Pyi - Wy));
////            if (Diw < 0.01) {
////                //System.out.println("ERR! Diw<0.01. Diw " + Diw + " Pxi: " + Pxi + " Wx: " + Wx);
////                Diw = 1;
////            }
//
//            double RD = Rw - Diw;
//
//            double Niwx = (Pxi - Wx) / Diw;
//            double Niwy = (Pyi - Wy) / Diw;
//
//            double Tiwx = -Niwy;    //Tangential component vector 
//            double Tiwy = Niwx;
//
//            double DeltaVtji = Vxi * Tiwx + Vyi * Tiwy;
//
//            double fsr = Aw * Math.exp(RD / Bw);
//            double fp = kw * g(RD);
//            double ff = kappaw * g(RD) * DeltaVtji;
//
//            fiwx += (fsr + fp) * Niwx + ff * Tiwx;
//            fiwy += (fsr + fp) * Niwy + ff * Tiwy;
//
//        }
//        
//        
//        double Vxwall = (fiwx) / Mi * timeStep;
//        double Vywall = (fiwy) / Mi * timeStep;

        // #################################################################
        // BOUNDARY FORCE FIELD
        // #################################################################
//        int N0x = 10 * PropertySet.WORLDXSIZE;        // Total number of cell in X direction
////        System.out.println(PropertySet.WORLDXSIZE);
//        int N0y = 10 * PropertySet.WORLDYSIZE;        // Total number of cell in Y direction
//        double Xmin = -10;                              // Minimum x point of terrain
//        double Xmax = PropertySet.WORLDXSIZE+5;         // Maximum x point of terrain (+5 is for buffer zone)
//        double Ymin = -10;                              // Minimum y point of terrain
//        double Ymax = PropertySet.WORLDYSIZE+5;         // Maximum y point of terrain

        double[] Fwi = averageSurroundForce(Pxi, Pyi, Xmax, Ymax, N0x, N0y, Fw, Fwx, Fwy);
        double fiwx = Fwi[0];
        double fiwy = Fwi[1];
//        System.out.println(fiwx);
//        System.out.println(fiwy);
        double Vxwall = (fiwx) / Mi * timeStep;
        double Vywall = (fiwy) / Mi * timeStep;

//        System.out.println("fijx: " + fijx);
//        System.out.println("fijy: " + fijy);
//        System.out.println("fiwx: " + fiwx);
//        System.out.println("fiwy: " + fiwy);
        
        // #################################################################
        // SUM AND BOUND TOTAL FORCE
        // #################################################################
//        System.out.println("(" + preferredVelocity.x + ", " + preferredVelocity.y+") ");
//        System.out.println("(" + Vxint + ", " + Vyint+") ");
        //if (Vxwall>5 || Vywall>5){
//            System.out.println("(" + fiwx + ", " + fiwy + ") ");
//            System.out.println("(" + Vxint + ", " + Vyint+") ");
//        }
//        System.out.println();
        
        double Vx = forceBound(preferredVelocity.x + Vxint + Vxwall);
        double Vy = forceBound(preferredVelocity.y + Vyint + Vywall);

        //System.out.println(Vxint);

        return new Vector2d(Vx, Vy);
    }
}
