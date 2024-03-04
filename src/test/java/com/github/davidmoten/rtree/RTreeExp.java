package com.github.davidmoten.rtree;

import static com.github.davidmoten.rtree.Entries.entry;
import static com.github.davidmoten.rtree.geometry.Geometries.circle;
import static com.github.davidmoten.rtree.geometry.Geometries.line;
import static com.github.davidmoten.rtree.geometry.Geometries.point;
import static com.github.davidmoten.rtree.geometry.Geometries.rectangle;
import static com.github.davidmoten.rtree.geometry.Intersects.pointIntersectsCircle;
import static com.github.davidmoten.rtree.geometry.Intersects.rectangleIntersectsCircle;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.guavamini.Lists;
import com.github.davidmoten.guavamini.Sets;
import com.github.davidmoten.rtree.fbs.FactoryFlatBuffers;
import com.github.davidmoten.rtree.geometry.Circle;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.HasGeometry;
import com.github.davidmoten.rtree.geometry.Intersects;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.internal.EntryDefault;
import com.github.davidmoten.rtree.internal.Functions;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.GroupedObservable;

import java.util.Random;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RTreeExp {

    @Test
    public void T_update_R_tree(){
        int numEntry = 1000;
        RTree<Double, Rectangle> tree = buildRTree(numEntry);
        // test time
        long acc = 0;
        int testNum = 10000;
        for(int i = 0; i < testNum; i++){
            long startTime = System.nanoTime();
            // 执行函数
            Random random = new Random();
            double ran_dev = (double)(random.nextInt(10) + 1);
            double ran_data = random.nextDouble();
            double ran_timestamp = random.nextDouble();
            Entry<Double, Rectangle> entry = e(ran_data, ran_dev * 10, ran_timestamp);
            tree = tree.add(entry);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);
            //System.out.println(duration);
            acc += duration;
            tree = tree.delete(entry);
        }
        System.out.println("T_update_R_tree of " + numEntry + " nodes' RTree: " + acc / testNum + " nanoseconds");
        assertEquals(numEntry, tree.size());
    }


    @Test
    public void T_search_R_tree_Q_1_1(){
        int numEntry = 1000;
        RTree<Double, Rectangle> tree = buildRTree(numEntry);
        System.out.println("tree depth:" + tree.calculateDepth());
        System.out.println(tree.asString());
        tree.visualize(1000, 1000).save(new File("target/rawdata.png"), "PNG");
        // test time
        long acc = 0;
        int testNum = 10000;
        Random random = new Random();
        for(int i = 0; i < testNum; i++){
            int dev = random.nextInt(10) + 1;
            int ti = random.nextInt(numEntry / 10) + 1; // 每个时间点对应10个设备，时间范围 = numEntry / 10
            Rectangle searchRange = eTor(dev * 10, 1e-6 * ti);
            long startTime = System.nanoTime();
            tree.search(searchRange);
            //System.out.println(tree.search(searchRange).toList().toBlocking().single());
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);
            acc += duration;
        }
        System.out.println("T_search_R_tree_Q_1_1 of " + numEntry + " nodes' RTree: " + acc / testNum + " nanoseconds");
        assertEquals(numEntry, tree.size());
    }

    @Test
    public void tabnum(){
        System.out.println("111\n111\t111\r111");
    }

    @Test
    public void T_search_R_tree_Q_1_10(){
        int numEntry = 1000;
        RTree<Double, Rectangle> tree = buildRTree(numEntry);
        System.out.println("tree depth:" + tree.calculateDepth());
        String tree_struct = tree.asString();
        //System.out.println(tree_struct);
        tree.visualize(1000, 1000).save(new File("target/rawdata.png"), "PNG");
        // test time
        long acc = 0;
        int testNum = 10000;
        Random random = new Random();

        String[] lines = tree_struct.split("\\r?\\n");
        ArrayList<String> precedingLines = new ArrayList<>();

        // Iterate through each line
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Check if the line starts with "entry"
            if (line.trim().startsWith("entry")) {
                // Add the preceding line to the precedingLines list
                if (i > 0) { // Ensure there is a preceding line
                    precedingLines.add(lines[i - 1]);
                }
            }
        }
        // Convert the ArrayList to an array
        String[] precedingArray = precedingLines.toArray(new String[0]);

        for(int i = 0; i < testNum; ){
            int lNum = random.nextInt(precedingArray.length);
            Rectangle searchRange = getEntryRange(precedingArray[lNum]);

            int getEntryNum = tree.search(searchRange).toList().toBlocking().single().size();
            //System.out.println("searched entry number: " + getEntryNum);
            //System.out.println("searched entry: " + tree.search(searchRange).toList().toBlocking().single());

            long startTime = System.nanoTime();
            tree.search(searchRange);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);

            if(getEntryNum == 10){
                acc += duration;
                i ++;
            }
        }
        System.out.println("T_search_R_tree_Q_1_10 of " + numEntry + " nodes' RTree: " + acc / testNum + " nanoseconds");
        assertEquals(numEntry, tree.size());
    }

    @Test
    public void T_search_R_tree_Q_5_1(){
        int numEntry = 100000;
        RTree<Double, Rectangle> tree = buildRTree(numEntry);
        System.out.println("tree depth:" + tree.calculateDepth());
        String tree_struct = tree.asString();
        String[] lines = tree_struct.split("\\r?\\n");
        //System.out.println(tree_struct);
        //tree.visualize(1000, 1000).save(new File("target/rawdata.png"), "PNG");

        List<String> result = new ArrayList<>();
        int entryIndentation = -1;

        // 找到 entry 所在行，并记录其缩进量
        for (String line : lines) {
            if (line.trim().startsWith("entry")) {
                entryIndentation = getIndentation(line);
                break;
            }
        }

        // 再次遍历整个字符串，将符合条件的 mbr 行添加到结果中
        for (String line : lines) {
            int indentation = getIndentation(line);
            if (line.trim().startsWith("mbr") && indentation == entryIndentation - 4) {
                result.add(line);
            }
        }

        String[] resultArray = result.toArray(new String[0]);

        // test time
        long acc = 0;
        int testNum = 10000;
        Random random = new Random();


        for(int i = 0; i < testNum; i++){
            int lNum = random.nextInt(resultArray.length);
            Rectangle searchRange = getEntryRange(resultArray[lNum]);

            int getEntryNum = tree.search(searchRange).toList().toBlocking().single().size();
            System.out.println("searched entry number: " + getEntryNum);
            System.out.println("searched entry: " + tree.search(searchRange).toList().toBlocking().single());

            long startTime = System.nanoTime();
            tree.search(searchRange);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);


            acc += duration;


        }
        System.out.println("T_search_R_tree_Q_5_1 of " + numEntry + " nodes' RTree: " + acc / testNum + " nanoseconds");
        assertEquals(numEntry, tree.size());
    }


    static int getIndentation(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    @Test
    public void testString(){
        String input = "mbr=Rectangle [x1=20.0, y1=7.7E-5, x2=21.0, y2=8.41E-5]";
        Rectangle rec = getEntryRange(input);
        System.out.println(rec);
    }

    // Here e refers to build an object that represents a data point, it
    // contains 3 parameters. "d" as data value; "num" as number of device;
    // "timestamp" as the time of data
    static Entry<Double, Rectangle> e(double d, double devNum, double timestamp) {
        return Entries.<Double, Rectangle>entry(d, r(devNum, timestamp));
    }
    static Rectangle eTor(double devNum, double timestamp) {
        return r(devNum, timestamp);
    }
    // Here r refers to m
    private static Rectangle r(double n, double t) {
        return rectangle(n, t, n + 1, t + 1e-7);
    }
    private static Rectangle r2(double n, double t, double n1, double t1) {
        return rectangle(n, t, n1, t1);
    }
    static RTree<Double, Rectangle> buildRTree(int numEntry){
        RTree<Double, Rectangle> tree = RTree.minChildren(8).maxChildren(14).create();
        // RTree<Object, Geometry>
        // tree.add
        // tree.delete
        // tree.search
        double[][] data = readCSVFile();
        int cnt = 0;
        int rows = data.length;
        int cols = data[0].length;
        /*
        for(int col = 2; col < cols; col++){
            for(int row = 1; row < rows; row++){
                Entry<Double, Rectangle> entry = e(data[row][col], 10 * (col - 1), data[row][1]);
                tree = tree.add(entry);
                cnt++;
                if(cnt >= numEntry) break;
            }
            if(cnt >= numEntry) break;
        }
         */

        for(int row = 1; row < rows; row++){
            for(int col = 2; col < cols; col++){
                Entry<Double, Rectangle> entry = e(data[row][col], 10 * (col - 1), data[row][1]);
                tree = tree.add(entry);
                cnt++;
                if(cnt >= numEntry) break;
            }
            if(cnt >= numEntry) break;
        }
        System.out.println("tree size: " + tree.size());
        return tree;
    }
    static Rectangle getEntryRange(String line){
        // 定义正则表达式
        // String regex = "(x1|y1|x2|y2)=(-?\\d+(\\.\\d+)?)";
        String regex = "(x1|y1|x2|y2)=(-?\\d+(\\.\\d+)?(?:E-?\\d+)?)";

        // 创建 Pattern 对象
        Pattern pattern = Pattern.compile(regex);
        // 创建 Matcher 对象
        Matcher matcher = pattern.matcher(line);
        Double x1 = null, y1 = null, x2 = null, y2 = null;
        // 遍历匹配结果
        while (matcher.find()) {
            String key = matcher.group(1); // 获取键名
            Double value = Double.parseDouble(matcher.group(2)); // 获取键值并转换为 Double 类型
            // 将键值存储到相应的变量中
            switch (key) {
                case "x1":
                    x1 = value;
                    break;
                case "y1":
                    y1 = value;
                    break;
                case "x2":
                    x2 = value;
                    break;
                case "y2":
                    y2 = value;
                    break;
            }
        }
        // System.out.println("x1: " + x1 + " x2: " + x2 + " y1: " + y1 + " y2 " + y2);
        return r2(x1, y1, x2, y2);
    }

    static double[][] readCSVFile(){
        String csvFile = "C:\\Users\\zpwang\\Desktop\\0_master\\Emnets\\基于区块链的物联网数据安全架构\\rawdata.csv";
        // 创建 BufferedReader 对象用于读取文件
        int rows = 0, cols = 12;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;

            // 读取文件每一行直到结束
            while ((line = br.readLine()) != null) {
                rows++;
                // 使用逗号分隔每一行的数据
                String[] data = line.split(",");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        double[][] data = new double[rows][cols];
        // 重新读取CSV文件并填充二维数组
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int row = 0;
            while ((line = br.readLine()) != null) {
                if(row == 0){row++; continue;}
                String[] values = line.split(",");
                for (int col = 1; col < cols; col++) {
                    data[row][col] = Double.parseDouble(values[col]);
                }
                row++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
