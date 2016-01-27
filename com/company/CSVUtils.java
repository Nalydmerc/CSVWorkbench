package com.company;

import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Creates a HashMap so that values can be looked up easily when
     * piecing together CSVs that may not be in the same order.
     * @param csv
     * @param primaryKeyColumn Key you will search for
     */
    public static HashMap<String, String[]> createMapFrom(CSV csv, int primaryKeyColumn) {
        HashMap<String, String[]> map = new HashMap<>();

        for (String[] row : csv.getContent()) {
            map.put(row[primaryKeyColumn], row);
        }

        return map;
    }


    /**
     * CombineCSVs
     * For when you have two CSVs that have data referencing the same objects that can be used as Primary Keys,
     * and want to combine them together into one. When using this, the values in the CSV do not have to be an any
     * particular order because the program looks for the common value being referenced.
     * @param mainCSV CSV you want to add data to
     * @param secondaryCSV CSV you're adding data from
     * @param mainPK Column of the Primary key (common refferenced value) to reference when looking for values.
     * @param secondaryPK Column of the Primary key (common refferenced value) to reference when looking for values.
     * @param columnToAdd Column of the secondary CSV to add to the mainCSV.
     */

    public static CSV combineCSVs(CSV mainCSV, CSV secondaryCSV, int mainPK, int secondaryPK, int columnToAdd) {

        HashMap<String, String[]> locationMap = CSVUtils.createMapFrom(secondaryCSV, secondaryPK); //PK of secondary

        CSV toWrite = CSV.createNew(mainCSV.createNewFileWithPrefix("Combined"));

        for (String[] result : mainCSV.getContent()) {
            String pk = result[mainPK]; //PrimaryKey
            String[] locationMapEntry = locationMap.get(pk);
            String additionalValue = "";

            if (locationMapEntry != null) {
                additionalValue = locationMapEntry[columnToAdd]; //Column of secondaryCSV
            } else {
                additionalValue = "NA";
            }

            ArrayList ral = new ArrayList(Arrays.asList(result));
            ral.add(additionalValue);
            String[] toWriteRow = (String[]) ral.toArray(new String[ral.size()]);
            toWrite.add(toWriteRow);
        }

        return toWrite;
    }
}
