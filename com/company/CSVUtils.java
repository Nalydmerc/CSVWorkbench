package com.company;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * @author Nalydmerc
 *         <p>
 *         CSV Utility to read and write CSV files.
 */

public class CSVUtils {

    /**
     * Get path from user. Accounts for quotations that Windows adds to "Copy as Path."
     * @param message to tell user when requesting path
     * @return user input path.
     */
    public static String requestPath(String message) {
        Scanner in = new Scanner(System.in);
        System.out.println(message);
        String path = in.nextLine();
        if (path.startsWith("\"")) {
            path = path.substring(1,path.length()-1);
        }
        return path;
    }

    public static HashMap<String, String[]> createMapFrom(String path, int primaryKeyColumn) {
        ArrayList<String[]> rows = getCSV(path);
        return createMapFrom(rows, primaryKeyColumn);
    }

    /**
     * Creates a HashMap so that values can be looked up easily when piecing together CSVs that may not be in the same order.
     */
    public static HashMap<String, String[]> createMapFrom(ArrayList<String[]> rows, int primaryKeyColumn) {
        HashMap<String, String[]> map = new HashMap<>();

        for (String[] row : rows) {
            map.put(row[primaryKeyColumn], row);
        }

        return map;
    }

    /**
     * Write to CSV file
     * @param path to file. Will create new if !exist
     * @param lines List of rows, each row is an array of columns.
     */
    public static void writeCSV(String path, ArrayList<String[]> lines) {
        String fileName = path.substring(path.lastIndexOf("\\") + 1);
        System.out.println("[CSVUtils] Writing File: " + fileName + " ...");
        try {
            PrintWriter writer = new PrintWriter(path);
            for (String[] line : lines) {
                String toWrite = "\"";
                for (String val:line) {
                    if (val == null) {
                        val = "";
                    }
                    val = val.replace("\"", "\"\"");
                    toWrite += val;
                    toWrite += "\",\"";
                }
                toWrite = toWrite.substring(0, toWrite.length()-2);
                writer.println(toWrite);
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("[CSVUtils] Done");
    }


    /**
     * Read CSV file into an ArrayList<String[]>
     * @param path to CSV
     * @return List of rows, each row is an array of columns.
     */
    public static ArrayList<String[]> getCSV(String path) {
        String fileName = path.substring(path.lastIndexOf("\\") + 1);
        System.out.println("[CSVUtils] Reading file: " + fileName + " ...");
        ArrayList<String[]> csvFile = new ArrayList<>();
        BufferedReader br = null;
        String line;

        try {
            BufferedReader readFirstList = new BufferedReader(new FileReader(path));
            String firstLine = readFirstList.readLine();
            String[] separated = firstLine.split(",");
            int numColumns = separated.length;

            br = new BufferedReader(new FileReader(path));
            while ((line = br.readLine()) != null) {

                ArrayList<String> columns = new ArrayList<>();
                String value = "";
                boolean inQuotes = false;
                for (int i = 0; i < line.length(); i++) {
                    char next = line.charAt(i);
                    if (next == '"') { //Quotations always imply quotation wrapped value
                        if (inQuotes) {

                            boolean endVal = false;

                            if (line.length() > i+1) {
                                if (line.charAt(i + 1) == '"') {
                                    //Quotation marks inside of a value are escaped by a second quotation mark
                                    value += next;
                                    i++;
                                } else {
                                    endVal = true;
                                }
                            } else {
                                //End of value at end of line
                                endVal = true;
                            }

                            if (endVal) {
                                inQuotes = false;
                                columns.add(value);
                                value = "";
                                i++;
                            }

                        } else {
                            //Quotation mark is the start of a quotation-wrapped value
                            inQuotes = true;
                        }
                    } else if (next == ',') { //No quotes, comma separated value
                        if (!inQuotes) {
                            columns.add(value);
                            value = "";
                        } else {
                            value += next;
                        }
                    } else if (line.length() == i + 1) { //No quotes, end of line.
                        if (inQuotes) {
                            //Multi-line quotation-wrapped value. Add next line to current.
                            value += next + " ";
                            line += br.readLine();
                        } else {
                            value += next;
                            columns.add(value);
                            value = "";
                        }
                    } else {
                        value += next;
                    }
                }
                csvFile.add(columns.toArray(new String[numColumns]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("[CSVUtils] Done");
        return csvFile;
    }
}
