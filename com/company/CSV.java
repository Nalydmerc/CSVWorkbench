package com.company;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * @author Nalydmerc
 * Created 1/27/2016
 *
 * CSV Object Library for reading, combining, altering, and creating CSVs.
 * //TODO Store content as ArrayList<Dictonary> instead of ArrayList<String>???
 *
 */
public class CSV {

    private File file;
    private String[] headers;
    private ArrayList<String[]> content;

    private CSV() {
    }

    /**
     * Read CSV file from file. Extra points if you manage to read in something that isn't a CSV, consequently defying
     * the logic of the program and confusing the heck out of me as to how you did it.
     * @param file existing file of CSV
     */
    public CSV(File file) {
        this.file = file;
        String path = file.getPath();
        String fileName = path.substring(path.lastIndexOf("\\") + 1);
        System.out.println("[CSV] Reading file: " + fileName + " ...");
        ArrayList<String[]> csvContent = new ArrayList<>();
        BufferedReader br = null;
        String line;

        try {
            br = new BufferedReader(new FileReader(file));
            int lineNum = 0;
            int numColumns = 0;

            while ((line = br.readLine()) != null) {

                ArrayList<String> columns = new ArrayList<>();
                String value = "";
                boolean inQuotes = false;
                //We read the line character by character to take the properties of CSV files into account
                for (int i = 0; i < line.length(); i++) {
                    char next = line.charAt(i);
                    if (next == '"') { //Quotations always imply quotation wrapped value
                        if (inQuotes) {

                            boolean endVal = false;

                            if (line.length() > i + 1) {
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
                if (lineNum == 0) {
                    numColumns = columns.size();
                    headers = columns.toArray(new String[numColumns]);
                } else {
                    csvContent.add(columns.toArray(new String[numColumns]));
                }
                lineNum++;
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
        this.content = csvContent;
    }

    public static CSV createNew(File file) {
        CSV csv = new CSV();
        csv.content = new ArrayList<String[]>();
        csv.file = file;
        return csv;
    }

    /**
     * Get the index of a String header in the array of headers.
     *
     * @param headerName Name of the String header you're searching for.
     * @return the int index of the header. Return -1 if it cannot be found. You outta luck, holmes.
     */
    public int getHeaderIndex(String headerName) {
        int id = -1;
        for (int i = 1; i < headers.length; i++) {
            String head = headers[i];
            if (head.toLowerCase().contains(headerName)) {
                return i;
            }
        }
        return id;
    }

    /**
     * Dump all data into file.
     * The CSV will probably be sad when you dump it, but after about a week of crying into its pillow
     * it'll realize there are plenty of fish in the sea. The CSV might develop some hard feelings toward you, though.
     *
     */
    public void dump() {

        boolean complete = false;

        do {
            try {
                PrintWriter writer = new PrintWriter(file);
                ArrayList<String[]> fullCSVtoWrite = content;
                fullCSVtoWrite.add(0, headers);


                for (String[] line : fullCSVtoWrite) {
                    String toWrite = "\"";
                    for (String val : line) {
                        if (val == null) {
                            val = "";
                        }
                        val = val.replace("\"", "\"\"");
                        toWrite += val;
                        toWrite += "\",\"";
                    }
                    toWrite = toWrite.substring(0, toWrite.length() - 2);
                    writer.println(toWrite);
                }
                writer.close();
                complete = true;
            } catch (FileNotFoundException e) {
                System.out.print("Please close the already open excel sheet. Press Enter to continue.");
                Scanner in = new Scanner(System.in);
                in.nextLine();
            }
        } while (!complete);
        System.out.println("[CSV] Done");
    }

    public String getPath() {
        return file.getPath();
    }


    /**
     * @return Containing folder of file including slash,
     * such that getFolderPath() + getFileName() + getExtension = file.getPath()
     */
    public String getFolderPath() {
        String path = getPath();
        String folder = path.substring(0, path.lastIndexOf("\\") + 1);
        return folder;
    }

    /**
     * @return Name of file without extension,
     * such that getFolderPath() + getFileName() + getExtension = file.getPath()
     */
    public String getFileName() {
        String path = getPath();
        String fileName = path.substring(path.lastIndexOf("\\") + 1, path.lastIndexOf('.'));
        return fileName;
    }

    /**
     * @return dot and extension of file,
     * such that getFolderPath() + getFileName() + getExtension = file.getPath()
     */
    public String getExtension() {
        String path = getPath();
        String extension = path.substring(path.lastIndexOf('.'));
        return extension;
    }

    /**
     * Quick way to create a new file with a prefixed filename, to use when making a new CSV.
     *
     * @param prefix String that will be prefixed to the filename of the returned file.
     * @return the newly created file, ready to be used for making a new CSV.
     */
    public File createNewFileWithPrefix(String prefix) {

        String newpath = getFolderPath() + prefix + getFileName() + getExtension();
        File f = new File(newpath);

        try {
            if (!f.exists()) {
                f.createNewFile();
            } else {
                f.delete();
                f.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
    }

    public File getFile() {
        return file;
    }

    public ArrayList<String[]> getContent() {
        return content;
    }

    public String[] getHeaders() {
        return headers;
    }

    public void setHeaders(ArrayList<String> newHeaders) {
        headers = newHeaders.toArray(new String[newHeaders.size()]);
    }

    public void setHeaders(String[] newHeaders) {
        headers = newHeaders;
    }

    public void addHeader(String newHeader) {
        ArrayList<String> newHeaders = new ArrayList<String>(Arrays.asList(headers));
        newHeaders.add(newHeader);
        headers = newHeaders.toArray(new String[newHeaders.size()]);
    }

    /**
     * Appends a String[] row to the end of CSV.content
     * @param row to append.
     */
    public void add(String[] row) {
        content.add(row);
    }
}
