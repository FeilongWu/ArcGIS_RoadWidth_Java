package net.mapsay.polygon.service;


//import org.apache.logging.log4j.core.config.ConfigurationSource;
//import org.apache.logging.log4j.core.config.Configurator;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryClipper;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoadWidthService {
    //private static Logger logger = LoggerFactory.getLogger(RoadWidthService.class);
    /**
     * @param fieldName 字段名称
     * @param clazz     字段类型
     */

    public static void addField(String path, String fieldName, Class clazz) throws IOException {
        File file = new File(path);
        ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createDataStore(new File(file.getPath()).toURI().toURL());
        String typeName = ds.getTypeNames()[0];
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(typeName, Transaction.AUTO_COMMIT);
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.init(writer.getFeatureType());
        builder.add(fieldName, clazz);
        SimpleFeatureType newstf = builder.buildFeatureType();
        //----------------------------------------------------------------------
        SimpleFeatureType schema = ds.getSchema();
        builder.setSuperType((SimpleFeatureType) schema.getSuper());
        builder.setName(schema.getName()); // get the name of shapefile without .shp
        builder.addAll(schema.getAttributeDescriptors());
        SimpleFeatureType nSchema = builder.buildFeatureType(); // build new schema
        List<SimpleFeature> features = new ArrayList<>();
        try (SimpleFeatureIterator itr = ds.getFeatureSource().getFeatures().features()) {
            while (itr.hasNext()) {
                SimpleFeature f = itr.next();
                SimpleFeature f2 = DataUtilities.reType(nSchema, f);
                features.add(f2);
            }
        }
        Map<String, String> connect = new HashMap();
        connect.put("url", file.toURI().toString());
        DataStore dataStore = DataStoreFinder.getDataStore(connect);
        SimpleFeatureSource source = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
        ds.createSchema(newstf);
        //logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>newstf:" + newstf);
        if (source instanceof SimpleFeatureStore) {
            SimpleFeatureStore store = (SimpleFeatureStore) source;
            store.addFeatures(DataUtilities.collection(features));
        } else {
            System.out.println("Unable to write to database");
        }
    }

    public static void UpdateField(String path, String fieldName, Class clazz,List<Double> widths) throws IOException {
        int sizeofWidths=widths.size();
        File file = new File(path);
        ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createDataStore(new File(file.getPath()).toURI().toURL());
        int count=0;
        String typeName = ds.getTypeNames()[0];
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(typeName, Transaction.AUTO_COMMIT);
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.init(writer.getFeatureType());
        SimpleFeatureType newstf = builder.buildFeatureType();
        //----------------------------------------------------------------------
        SimpleFeatureType schema = ds.getSchema();
        builder.setSuperType((SimpleFeatureType) schema.getSuper());
        builder.setName(schema.getName()); // get the name of shapefile without .shp
        builder.addAll(schema.getAttributeDescriptors());
        SimpleFeatureType nSchema = builder.buildFeatureType(); // build new schema
        List<SimpleFeature> features = new ArrayList<>();
        try (SimpleFeatureIterator itr = ds.getFeatureSource().getFeatures().features()) {
            while (itr.hasNext()) {
                SimpleFeature f = itr.next();
                f.setAttribute(fieldName,widths.get(count));
                SimpleFeature f2 = DataUtilities.reType(nSchema, f);
                count++;
                features.add(f2);
                if (count==sizeofWidths){break;}
            }
        }
        Map<String, String> connect = new HashMap();
        connect.put("url", file.toURI().toString());
        DataStore dataStore = DataStoreFinder.getDataStore(connect);
        SimpleFeatureSource source = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
        ds.createSchema(newstf);
        if (source instanceof SimpleFeatureStore) {
            SimpleFeatureStore store = (SimpleFeatureStore) source;
            store.addFeatures(DataUtilities.collection(features));
        } else {
            System.out.println("Unable to write to database");
        }
    }


    public static double Max(Double a, Double b){
        return (a>b?a:b);
    }

    public static double Min(Double a, Double b){
        return (a<b?a:b);
    }

    private static boolean Overlap(List<Double> collection){
        //the collection consists of four doubles:start1, end1, start2, and end2.
        // check if these points have overlapping part.
        if ((collection.get(2)>Min(collection.get(0),collection.get(1)))&(collection.get(2)<Max(collection.get(0),collection.get(1)))){
            return true;
        }
        else if ((collection.get(3)>Min(collection.get(0),collection.get(1)))&(collection.get(3)<Max(collection.get(0),collection.get(1)))){
            return true;
        }
        else if ((collection.get(0)>Min(collection.get(2),collection.get(3)))&(collection.get(0)<Max(collection.get(2),collection.get(3)))){
            return true;
        }
        else if ((collection.get(1)>Min(collection.get(2),collection.get(3)))&(collection.get(1)<Max(collection.get(2),collection.get(3)))){
            return true;
        }
        return false;
    }

    private static boolean RunOn(Double S1X,Double S1Y,Double E1X, Double E1Y,Double S2X,Double S2Y,Double E2X, Double E2Y){
        // S=start, E=end, 1=line1, 2=line2, X=x.coordinate Y=y.coordinate
        //it will return true or false depending on the two lines having overlapping length>0
        List<Double> collection=new ArrayList<Double>();
        if ((S1Y-E1Y)==0.0&(E1Y-S2Y)==0.0&(S2Y-E2Y)==0.0){
            collection.add(S1X);collection.add(E1X);collection.add(S2X);collection.add(E2X);
            if (Overlap(collection)){ return true;}else{return  false;}
        }else if((S1X-E1X)==0.0&(E1X-S2X)==0.0&(S2X-E2X)==0.0){
            collection.add(S1Y);collection.add(E1Y);collection.add(S2Y);collection.add(E2Y);
            if (Overlap(collection)){return true;}else{return false;}
        }
        return false;
    }

    public static Polygon StringToPolygon (String WKT){
        //this method takes WKT as coordinates without parentheses from which a polygon is created
        // as a returned value
        GeometryFactory gf = new GeometryFactory();
        ArrayList<Coordinate> soln = new ArrayList<>();
        String[] sliptWKT = WKT.split(",");
        int count = 0;
        Double X, Y;
        for (String a : sliptWKT) {
            if (count == 0) {
                X = Double.parseDouble(a.split(" ")[0]);
                Y = Double.parseDouble(a.split(" ")[1]);
                soln.add(new Coordinate(X, Y));
            } else {
                X = Double.parseDouble(a.substring(1).split(" ")[0]);
                Y = Double.parseDouble(a.substring(1).split(" ")[1]);
                soln.add(new Coordinate(X, Y));
            }
            count++;
        }
        org.locationtech.jts.geom.Polygon poly = gf.createPolygon(soln.toArray(new Coordinate[]{}));
        return poly;
    }

    public static Geometry genPolygon(String WKT) {
        // takes the wkt of a polygon and return it as "polygon" type
            WKT = WKT.substring(16 + WKT.indexOf("MULTIPOLYGON"), WKT.length() - 3);
            try {
            return StringToPolygon(WKT).reverse();
        }catch (Exception e){
                Geometry[] polygonList;
                List<Polygon>interior = new ArrayList<Polygon>();
                String[] STR1;
                Geometry geo;
                String[] WKT1 = WKT.split("\\)"+"\\), "+"\\("+"\\(");
                polygonList=new Geometry[WKT1.length];
                for (int i=0;i<polygonList.length;i++){
                    STR1=WKT1[i].split("\\)"+", "+"\\(");
                    if (STR1.length>1) {
                        for (int j = 1; j < STR1.length; j++) {
                            interior.add(StringToPolygon(STR1[j]));
                        }
                        polygonList[i]= StringToPolygon(STR1[0]).reverse();
                        for (int k=0;k<interior.size();k++){
                            System.out.println();
                            try {
                                polygonList[i] = polygonList[i].difference(interior.get(k).reverse());
                            }catch (Exception e1){}
                        }
                    }else { polygonList[i]=StringToPolygon(STR1[0]).reverse();}
                }
                geo=polygonList[0];
                for (int i=1;i<polygonList.length;i++){
                    geo=geo.union(polygonList[i]);

                }
                return  geo;

            }
    }

    private static int getLineNum(double scope, double spacing) {
        // determine the number of separating lines and return it
        scope += 0.1;
        int lineNum;
        lineNum = (int) (scope / spacing);
        return lineNum + 2;
    }

    public static List genSeparating(GeometryAttribute bounding, double spacing) {
        double xmax, xmin, ymax, ymin, init;
        int lineNum; // number of lines
        List<Double> spLines = new ArrayList<Double>();
        xmax = bounding.getBounds().getMaxX();
        xmin = bounding.getBounds().getMinX();
        ymax = bounding.getBounds().getMaxY();
        ymin = bounding.getBounds().getMinY();
        if (Math.abs(xmax - xmin) >= Math.abs(ymax - ymin)) {
            if ((Math.abs(xmax - xmin) / 3.0) < 1) {
                spacing = (Math.abs(xmax - xmin) / 3.0);
            }
            lineNum = getLineNum(Math.abs(xmax - xmin), spacing);
            init = xmin - 0.01;
            for (int i = 0; i < lineNum; i++) {
                spLines.add(init + i * spacing);
                spLines.add(ymax);
                spLines.add(init + i * spacing);
                spLines.add(ymin);
            }
            return spLines;
        } else {
            if ((Math.abs(ymax - ymin) / 3.0) < 1) {
                spacing = (Math.abs(ymax - ymin) / 3.0);
            }
            lineNum = getLineNum(Math.abs(ymax - ymin), spacing);
            init = ymin - 0.01;
            for (int i = 0; i < lineNum; i++) {
                spLines.add(xmin);
                spLines.add(init + i * spacing);
                spLines.add(xmax);
                spLines.add(init + i * spacing);
            }
            return spLines;
        }
    }

    public static Geometry pointsToGeometry(List<Double> X, List<Double> Y){
        // this method takes the X and Y coordinates from which a geometry will be created.
        GeometryFactory gf = new GeometryFactory();
        ArrayList<Coordinate> soln = new ArrayList<>();
        int sizeX = X.size();
        for (int i = 0; i<sizeX;i++){
            soln.add(new Coordinate(X.get(i),Y.get(i)));
        }
        Polygon poly = gf.createPolygon(soln.toArray(new Coordinate[]{}));
        return poly.reverse();
    }

    private static List<Geometry> genGeometries (String WKT,Geometry g){
        try {
            List<Double> Coordinates1 = new ArrayList<Double>();
            String[] coordinates;
            Double[][] coorSet;// of the format [[x1,y1],[x2,y2],...]
            List<Double> Coordinates = new ArrayList<Double>(); // store the converted string coordinates
            List<Double> tempX = new ArrayList<Double>(); // store x coordinates, used to separate polygons
            List<Double> tempY = new ArrayList<Double>();
            List<Double> SubX = new ArrayList<Double>(); // store the temporary independent polygon's x coordinates
            List<Double> SubY = new ArrayList<Double>(); // store the temporary independent polygon's y coordinates
            List<Geometry> geometries = new ArrayList<Geometry>();// it is the result
            int num, sizeX, sizeY;
            String newWKT = WKT.substring(WKT.indexOf("((") + 2, WKT.length() - 2);
            newWKT = newWKT.replaceAll(",", "");
            coordinates = newWKT.split(" ");
            num = coordinates.length;
            if (num == 8) {
                geometries.add(g);
                return geometries;
            }
            for (String a : coordinates) {
                //the last XY coordinate is unnecessary;
                Coordinates1.add(Double.parseDouble(a));
            }
            // remove redundant points
            for (int i=0;i<Coordinates1.size()/2-1;i++){
                if ((Coordinates1.get(i*2)-Coordinates1.get(i*2+2)==0.0&(Coordinates1.get(i*2+1)-Coordinates1.get(i*2+3)==0.0))){
                    continue;
                }else{Coordinates.add(Coordinates1.get(i*2));Coordinates.add(Coordinates1.get(i*2+1));}
            }
            Coordinates.add(Coordinates.get(0));Coordinates.add(Coordinates.get(1));
            num=Coordinates.size();
            coorSet = new Double[num / 2][2];
            for (int i = 0; i <= num / 2 - 1; i++) {
                coorSet[i][0] = Coordinates.get(2 * i);
                coorSet[i][1] = Coordinates.get(2 * i + 1);
            }
            for (int i = 0; i < num / 2; i++) {
                tempX.add(coorSet[i][0]);
                tempY.add(coorSet[i][1]);
                sizeX = tempX.size();
                sizeY = tempY.size();
                if (sizeY > 2) {
                    for (int j = sizeX - 2; j > 0; j--) {
                        if (RunOn(tempX.get(j - 1), tempY.get(j - 1), tempX.get(j), tempY.get(j), tempX.get(tempX.size() - 2), tempY.get(tempY.size() - 2), tempX.get(tempX.size() - 1), tempY.get(tempY.size() - 1))) {
                            if (sizeX - j == 2) {
                                tempX.remove(j);
                                tempY.remove(j);
                            }//get rid of redundant points
                            else {
                                SubX = tempX.subList(j, tempX.size() - 1);
                                SubY = tempY.subList(j, tempY.size() - 1);
                                //close the line ring
                                SubX.add(SubX.get(0));
                                SubY.add(SubY.get(0));

                                geometries.add(pointsToGeometry(SubX, SubY));
                                tempX = tempX.subList(0, j);
                                tempY = tempY.subList(0, j);
                                tempX.add(coorSet[i][0]);
                                tempY.add(coorSet[i][1]);
                                break;
                            }
                        }
                    }
                }

            }
            sizeX = tempX.size();
            if (tempX.size() > 3) {


                for (int i = 0; i < sizeX - 2; i++) {
                    if (RunOn(tempX.get(i), tempY.get(i), tempX.get(i + 1), tempY.get(i + 1), tempX.get(i + 1), tempY.get(i + 1), tempX.get(i + 2), tempY.get(i + 2))) {
                        tempX.remove(i + 1);tempY.remove(i + 1);
                        geometries.add(pointsToGeometry(tempX, tempY)); return geometries;
                    }
                    else if (RunOn(tempX.get(i),tempY.get(i),tempX.get(i+1),tempY.get(i+1),tempX.get(sizeX-2),tempY.get(sizeX-2),tempX.get(sizeX-1),tempY.get(sizeX-1))){
                        tempX=tempX.subList(1,sizeX-1);tempY=tempY.subList(1,sizeX-1);tempX.add(tempX.get(0));tempY.add(tempY.get(0));
                        geometries.add(pointsToGeometry(tempX, tempY)); return geometries;
                    }
                }
                geometries.add(pointsToGeometry(tempX, tempY));
            }
            return geometries;}catch (Exception e){ return new ArrayList<Geometry>();}
    }

    public static List<Geometry> polygonToMultipolygon(Geometry g) {
        // it takes the WKT of a polygon and return the WKT representing the same feature in multipolygon
        // the polygon is generated by clipping a polygon using an envelope
        try {
            String WKT = g.toString();
        if (WKT.indexOf("), (")!=-1){
            String[] newWKT;
            List<Geometry> results=new ArrayList<Geometry>();
            WKT=WKT.substring(WKT.indexOf("((") + 1, WKT.length() - 2);
            newWKT=WKT.split("\\), "+"\\(");
            Geometry geo;
            for (int i=1;i<newWKT.length;i++){
                results.add(StringToPolygon(newWKT[i]).reverse());
            }
            geo=genGeometries(newWKT[0], g).get(0);
            try {
                for (int i = 0; i < results.size(); i++) {
                    geo = geo.difference(results.get(i));
                }
                results.clear();
                if (geo.toString().indexOf("MULTI")==-1){
                    return genGeometries(geo.toString(),g);
                }else {
                    newWKT=geo.toString().substring(geo.toString().indexOf("MULTIPOLYGON") + 16, geo.toString().length() - 3).split("\\)"+"\\), "+"\\("+"\\(");
                    for (int i=0;i<newWKT.length;i++){
                        results.add(StringToPolygon(newWKT[i]).reverse());
                    }
                    return  results;
                }
            }catch (Exception e1){
            }
        }
        return genGeometries(WKT,g);
        }catch (Exception e3){return  new ArrayList<Geometry>();

        }    }

    private static List createSmallPoly(Geometry polygon, List<Double> splines) {
        //this method takes the polygon of the road and coordinates of separating lines;
        // it returns a list of small polygons cut by the separating lines
        List<Geometry> smallPoly = new ArrayList<Geometry>();
        List<Geometry> geometries;
        Envelope env;
        GeometryClipper clipper;

        int num = splines.size() - 4; // number of vertices XY values
        for (int i = 0; i < num; i += 4) {
            env = new Envelope(splines.get(i), splines.get(i + 6), splines.get(i + 1), splines.get(i + 7));
            clipper = new GeometryClipper(env);
            geometries=polygonToMultipolygon(clipper.clip(polygon,false));
            if (geometries.size()<1){continue;}
            for (Geometry geo : geometries){
                smallPoly.add(geo);
            }
        }

        return smallPoly;
    }

    private static List createBuffer(List<Double> splines) {
        //returns a list of clippers of envelopes which are buffer shapes of splines.
        boolean isVertical = ((splines.get(0) - splines.get(2) == 0.0d));
        List<GeometryClipper> buffer = new ArrayList<GeometryClipper>();
        int num = splines.size() / 4; // the number of lines
        int start;
        if (isVertical) {
            for (int i = 0; i < num; i++) {
                start = 4 * i;
                buffer.add(new GeometryClipper(new Envelope(splines.get(start) - 0.01, splines.get(start) + 0.01, splines.get(start + 1), splines.get(start + 3))));
            }
            return buffer;
        } else {
            for (int i = 0; i < num; i++) {
                start = 4 * i;
                buffer.add(new GeometryClipper(new Envelope(splines.get(start), splines.get(start + 2), splines.get(start+1) - 0.01, splines.get(start+1) + 0.01)));
            }

            return buffer;
        }

    }

    private static int[] linspace(int start, int end, int size){
        // this method only works for integer type
        int[] index =new int[size];
        float step;
        step= ((end-start)/((float) size-1));
        for (int i=0;i<size;i++){
            index[i]=(int) (start+i*step);
        }
        return index;
    }

    private static Double calWidth(BoundingBox bounds, Geometry smallPoly,Geometry polygon ,List<Double> centroids, int lineNum){
        // this method returns the average width based on a single small polygon
        double a,b,slope,step,y2,y1,y3,x2,x1,x3,width;// a b are y =ax+b of the line connecting the centroids, while slope is the slope perpendicular to a
        // y2, x2 are the coordinates of the intersection point
        GeometryFactory gf = new GeometryFactory();
        Geometry[] polygonList=new Polygon[1];
        polygonList[0]=polygon;
        ArrayList<Coordinate> soln = new ArrayList<>();
        if ((centroids.get(2)-centroids.get(0))==0.0){a=(centroids.get(3)-centroids.get(1))/0.000001;}
        else if ((centroids.get(3)-centroids.get(1))==0.0){a=(0.000001/(centroids.get(2)-centroids.get(0)));}
        else{a=(centroids.get(3)-centroids.get(1))/(centroids.get(2)-centroids.get(0));}
        b=(centroids.get(1)-a*centroids.get(0));
        x1=bounds.getMinX();x3=bounds.getMaxX();
        slope=(-1/a);
        x2=Min(centroids.get(0),centroids.get(2));
        step=(Max(centroids.get(0),centroids.get(2))-x2)/(lineNum+1);
        width=0.0;

        for (int i=0;i<lineNum;i++){
            x2+=(i+1)*step;y2=x2*a+b;
            y1=y2-slope*(x2-x1);
            y3=y2+slope*(x3-x2);
            soln.add(new Coordinate(x1,y1));soln.add(new Coordinate(x2+0.000000001,y2+0.000000001));soln.add(new Coordinate(x3,y3));soln.add(new Coordinate(x1,y1));
            try {
                width += (gf.createLinearRing(soln.toArray(new Coordinate[]{})).reverse().intersection(polygon).getLength() - 0.000001) / 2;
            }catch (Exception e){
                width += (gf.createLinearRing(soln.toArray(new Coordinate[]{})).reverse().intersection(smallPoly).getLength() - 0.000001) / 2;
            }
        }
        return width/lineNum;


    }

    public static double getWidth(GeometryAttribute road, double spacing) {
        // this method takes the polygons as the indicated type
        // spacing is the distance between to adjacent separating lines
        // returns the average width of the polygon
        List<Double> area = new ArrayList<Double>();
        List<Double> widths = new ArrayList<Double>();
        double scope =(Max((road.getBounds().getMaxX()-road.getBounds().getMinX()),(road.getBounds().getMaxY()-road.getBounds().getMinY())));
        if (spacing>=scope){
            spacing=scope/3;
        }else  if (scope<=2*spacing){spacing=scope/6;}
        int limit=16; // the limit of the maximum number of polygons that will count toward the avg. width
        int[] index; //the index is a list  of integers that will be used to index small polygons
        Geometry polygon = genPolygon(road.toString());
        List splines = genSeparating(road, spacing);
        List<Geometry> smallPoly = createSmallPoly(polygon, splines);
        List<Geometry> ClipBuffer;
        List<GeometryClipper> lineBuffer = createBuffer(splines);
        List<Double> centroids = new ArrayList<Double>(); //stores centroid's XY coordinate, [x1,y1,x2,y2...]
        Double totArea=0.0;
        Double AreaTimesWidth=0.0;
        int lineNum =1; // this is the number of the perpendicular line(s)
         if ((smallPoly.size())>limit){
            index=linspace(0,smallPoly.size()-1,limit);
        }else{index=linspace(0,smallPoly.size()-1,smallPoly.size());}
       for (int i=0;i<index.length;i++){
           for (GeometryClipper env:lineBuffer) {
               try {
                   ClipBuffer=polygonToMultipolygon(env.clip(smallPoly.get(index[i]), false));
                   if (ClipBuffer.size()==2){break;}
                   else if (ClipBuffer.size()==1){centroids.add(ClipBuffer.get(0).getCentroid().getX());centroids.add(ClipBuffer.get(0).getCentroid().getY());}
               } catch (Exception e){
               }
           }
           if (centroids.size()==4){
               area.add(smallPoly.get(index[i]).getArea());
               widths.add(calWidth(road.getBounds(),smallPoly.get(index[i]),polygon,centroids,lineNum));
           }
           centroids.clear();
       }
       for (int i=0;i<area.size();i++){
           totArea+=area.get(i);
           AreaTimesWidth+=area.get(i)*widths.get(i);
       }
        return AreaTimesWidth/totArea;
    }




    public static void main(String Path) throws IOException {

        //ConfigurationSource source = new ConfigurationSource(RoadWidthService.class.getClassLoader().getResource("log4j2.xml").openStream());
        //Configurator.initialize(null, source);
        //logger.debug("===========================> test");
        String path=Path ; //"C:\\Users\\FEILONG WU\\Desktop\\Type0403\\road10Copy.shp";
        String attribute = "Avg_Width";
        addField(path, attribute, Double.class);
        File file = new File(path);


        try {
            Map<String, String> connect = new HashMap();
            connect.put("url", file.toURI().toString());
            DataStore dataStore = DataStoreFinder.getDataStore(connect);
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];
            FeatureSource featureSource = dataStore.getFeatureSource(typeName);
            FeatureCollection collection = featureSource.getFeatures();
            FeatureIterator iterator = collection.features();
            GeometryAttribute sourceGeometry;
            double spacing = 9;
            int count =0;
            List<Double> width = new ArrayList<Double>();
            try {
                while (iterator.hasNext()) {
                    //System.out.println("the nth road "+count);count++;
                    Feature feature = iterator.next();
                    sourceGeometry = feature.getDefaultGeometryProperty();
                    width.add(Math.round(getWidth(sourceGeometry, spacing)*10.0)/10.0);
                }

                //System.out.println("Width "+width.get(0));
                UpdateField(path, attribute, Double.class,width);
            } finally {
                iterator.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            //logger.error(e.getMessage(), e);
        }
    }
}
