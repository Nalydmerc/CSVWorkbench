package com.company;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class CSVUtils {

    public String requestPath(String message) {
        Scanner in = new Scanner(System.in);
        System.out.println(message);
        String path = in.nextLine();
        if (path.startsWith("\"")) {
            path = path.substring(1,path.length()-1);
        }
        return path;
    }

    public void writeCSV(String path, ArrayList<String[]> lines) {
        System.out.println("Writing File: " + path + "...");
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
        System.out.println("Done.");
    }


    /*  Read CSV file into an ArrayList<String[]>
    Results are recorded as:
    {value, value}
    {value, value}, etc.
    Such that every entry in the list is a line, where the line is an array containing it's columns.
*/
    public ArrayList<String[]> getCSV(String path) {
        String fileName = path.substring(path.lastIndexOf("\\")+1);
        System.out.println("Reading file: " + fileName + "...");
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
                    if (next == '"') {
                        if (inQuotes) {

                            boolean endVal = false;

                            if (line.length() > i+1) {
                                if (line.charAt(i+1) == '"') {
                                    value += next;
                                    i++;
                                } else {
                                    endVal = true;
                                }
                            } else {
                                endVal = true;
                            }

                            if (endVal) {
                                inQuotes = false;
                                columns.add(value);
                                value = "";
                                i++;
                            }

                        } else {
                            inQuotes = true;
                        }
                    } else if (next == ',') {
                        if (!inQuotes) {
                            columns.add(value);
                            value = "";
                        } else {
                            value += next;
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
        System.out.println("Done.");
        return csvFile;
    }
}
