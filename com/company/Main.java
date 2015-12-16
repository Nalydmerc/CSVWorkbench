
/*
 * CSV workbench
 * @author Nalydmerc@gmail.com
 *
 * This program is used for sorting and working with CSV files. It's designed to be used on the fly.
 *
 * ABOUT runProcessing():
 * Method reads from two CSVs. They need certain headers in their data to work properly.
 * The orders on the headers don't matter, but their names do. The program looks
 * for the names of the headers, with the exception of the first column in both CSVs. The first column must have a
 * UID for each search to link the search criteria with the results.
 * Required Headers must contain:
 *  CSV 1:
 *      "URL"(of the search results page), "Dealer Name", "Dealer No", "Dealer City"
 *  CSV 2 :
 *      "Name"(of listing result), "Link"(to listing page)
 *
 * Important note:  Special attributes to look for in the CSVs are included for Yahoo Local.
 *                  They are located in the organizeCSV method and will be marked with comments.
 */

package com.company;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static String[] dealerTypes = {
            "Honda", "Acura", "Toyota", "Ford", "Hyundai", "Dodge", "Chevrolet","Chrysler",
            "Jeep", "Lexus", "Nissan", "Fiat", "Lincoln", "Mazda", "Infinity", "Jaguar", "Bently"};

    public static String[] possibleExtensions = {"Results","Links","dealers"};
    public static String CurrentDealerType = "";
    public String fileWithoutExtension = "";
    public String extension = "";
    private CSVUtils csvUtils = new CSVUtils();

    /*
     * Remember, this is designed to be used on the fly. Code in main() will usually be for one time
     * CSV runs made for a specific purpose.
     */

    public static void main(String[] args) {
        Main main = new Main();

        ArrayList<String[]> emailCSV = main.csvUtils.getCSV("C:\\Users\\intern\\Desktop\\HondaDealerEmails.csv");
        ArrayList<String[]> baseCSV = main.csvUtils.getCSV("C:\\Users\\intern\\Documents\\InputDataSources\\Honda Dealer List.csv");

        main.csvUtils.writeCSV("C:\\Users\\intern\\Desktop\\email.csv", emailCSV);
        main.csvUtils.writeCSV("C:\\Users\\intern\\Desktop\\dealerlist.csv", baseCSV);

        ArrayList<String[]> toWrite = new ArrayList<>();
        HashMap<String, String> numNameMap = new HashMap<>();

        for (String[] row : baseCSV) {
            numNameMap.put(row[0], row[3]);
        }

        for (String[] row : emailCSV) {
            String url = row[0];
            String num = row[1];
            String dealerName = numNameMap.get(num);
            String eName = row[2];
            String pos = row[3];
            String email = row[4];

            toWrite.add(new String[] {url, num, dealerName, eName, pos, email});
        }

        main.csvUtils.writeCSV("C:\\Users\\intern\\Desktop\\AddedHondaDealerEmails.csv", toWrite);

    }

    public String[] findRegexIn(String text, String regexSubstring) {
        ArrayList<String> results = new ArrayList<>();
        Pattern reg = Pattern.compile(regexSubstring);
        Matcher matcher = reg.matcher(text);
        for (int i = 0 ; i < matcher.groupCount(); i++) {
            results.add(matcher.group(i));
        }
        return results.toArray(new String[results.size()]);
    }


    public String requestPath() {
        Scanner in = new Scanner(System.in);
        System.out.println("Enter File Path:");
        String path = in.nextLine();
        if (path.startsWith("\"")) {
            path = path.substring(1,path.length()-1);
        }
        return path;
    }


    /*
     * Used when scoring results based on a name.
     * TODO: Split this up and organize it.
     */

    public void runProcessing() {

        String path = requestPath();

        this.fileWithoutExtension = path.substring(0,path.length()-4);

        for (String testE : this.possibleExtensions) {
            if (this.doesExtensionExist(testE)) {
                this.extension = testE;
                // 10/10 variable naming practices in this section, Dylan.
                String thing = this.fileWithoutExtension.substring(this.fileWithoutExtension.lastIndexOf("\\"));
                thing += "_" + testE;
                System.out.println("Results file found: " + thing);
            }
        }

        String folderPath = path.substring(0, path.lastIndexOf("\\") + 1);
        Scanner in = new Scanner(System.in);
        if (this.extension == "") {
            System.out.println("Please choose results csv from list:");
            File folder = new File(folderPath);
            File[] listOfFiles = folder.listFiles();
            while (true) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].getName().contains("_")) {
                        System.out.println(i + ": " + listOfFiles[i].getName());
                    }
                }
                try {
                    int fileNo = Integer.parseInt(in.nextLine());
                    String correctFile = listOfFiles[fileNo].getName();
                    this.extension = correctFile.substring(correctFile.lastIndexOf("_") + 1,
                            correctFile.lastIndexOf(".csv"));
                    break;
                } catch (NumberFormatException ex) {
                    System.out.println("\nError: Input was not an integer!\n");
                }
            }
        }

        System.out.println("Enter Dealer Type:");
        CurrentDealerType = in.nextLine();

        System.out.println("Organizing CSV file...");
        this.organizeCSV(path);
        System.out.println("CSV Organized.");

        String fileName = path.substring(path.lastIndexOf("\\")+1);
        path = folderPath + "Organized" + fileName;

        ArrayList<String[]> csvFile = csvUtils.getCSV(path);
        ArrayList<String[]> procCsvFile = new ArrayList<>();

        procCsvFile.add(new String[]{"Search URL","Dealer No","Search Name","Result","Link"});

        int i = 1;

        System.out.println("Iterating through results...");

        while (i < csvFile.size()-1) {

            String[] line = csvFile.get(i);

            String startUrl = line[0];
            String dealerNo = line[1];
            String name = line[2];
            String city = line[3];

            ArrayList<String[]> results = new ArrayList<>();

            int resultIndex = 0;
            if (i + resultIndex < csvFile.size()) {
                while (csvFile.get(i+resultIndex)[0].equalsIgnoreCase(startUrl)) {
                    String[] result = {csvFile.get(i + resultIndex)[4], csvFile.get(i + resultIndex)[5]};
                    results.add(result);
                    if (i + resultIndex + 1 >= csvFile.size()) {
                        break;
                    }
                    resultIndex++;
                }
            } else {
                String[] result = {csvFile.get(i + resultIndex)[4], csvFile.get(i + resultIndex)[5]};
                results.add(result);
                resultIndex++;
            }


            Map<String[], Integer> resultsMap = this.processResults(name, city, this.CurrentDealerType, results);

            int highestScore = 0;

            for (int score : resultsMap.values()) {
                if (score > highestScore) {
                    highestScore = score;
                }
            }

            for (Map.Entry<String[], Integer> entry : resultsMap.entrySet()) {
                if (entry.getValue() >= highestScore-1) {

                    String[] toAdd = {startUrl, dealerNo, name, entry.getKey()[0], entry.getKey()[1]};
                    procCsvFile.add(toAdd);
                }
            }

            i += resultIndex;
        }

        folderPath = path.substring(0,path.lastIndexOf("\\")+1);
        fileName = path.substring(path.lastIndexOf("\\")+1);
        csvUtils.writeCSV(folderPath + "Processed" + fileName, procCsvFile);

    }

    public boolean doesExtensionExist(String extension) {

        String testFileExtension = fileWithoutExtension + "_" + extension + ".csv";
        File toTest = new File(testFileExtension);
        if (toTest.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public void organizeCSV(String path) {

        ArrayList<String[]> parentTemplate = csvUtils.getCSV(path);

        String toAddResults = path.substring(0, path.lastIndexOf("."));
        toAddResults += "_" + extension + ".csv";
        ArrayList<String[]> resultsTemplate = csvUtils.getCSV(toAddResults);

        ArrayList<String[]> toWrite = new ArrayList<String[]>();

        HashMap<String, String[]> parentMap = new HashMap<>();
        HashMap<String, ArrayList<String[]>> resultsMap = new HashMap<>();

        for (String[] row : parentTemplate) {
            parentMap.put(row[0], row);
        }

        for (String[] row : resultsTemplate) {
            if (resultsMap.containsKey(row[0])) {
                ArrayList<String[]> resultsForRow = resultsMap.get(row[0]);
                resultsForRow.add(row);
                resultsMap.put(row[0], resultsForRow);
            } else {
                ArrayList<String[]> resultsForRow = new ArrayList<>();
                resultsForRow.add(row);
                resultsMap.put(row[0], resultsForRow);
            }
        }

        int urlColumn = -1;
        int dealerNoColumn = -1;
        int dealerNameColumn = -1;
        int dealerCityColumn = -1;
        int yahooLocalNameColumn = -1; //Yahoo Local addition
        int yahooLocalLinkColumn = -1;
        boolean isYahooLocal = false;
        String[] headers = parentTemplate.get(0);

        for (int i = 1; i < headers.length; i++) {
            String head = headers[i];
            if (head.contains("Dealer No")) {
                dealerNoColumn = i;
            } else if (head.contains("Dealer Name")) {
                dealerNameColumn = i;
            } else if (head.contains("Dealer City")) {
                dealerCityColumn = i;
            } else if (head.contains("URL")) {
                urlColumn = i;
            } else if (head.equals("YahooLocalName")) { //Yahoo Local addition
                yahooLocalNameColumn = i;
                isYahooLocal = true;
            } else if (head.equals("YahooLocalLink")) {
                yahooLocalLinkColumn = i;
                isYahooLocal = true;
            }
        }

        if (urlColumn == -1 || dealerCityColumn == -1 || dealerNoColumn == -1 || dealerNameColumn == -1) {
            System.out.println("Error: Could not find column numbers from headers.");
            if (urlColumn == -1) {
                System.out.println("Could not find header that contains \"URL.\"");
            }
            if (dealerCityColumn == -1) {
                System.out.println("Could not find header that contains \"City.\"");
            }
            if (dealerNameColumn == -1) {
                System.out.println("Could not find header that contains \"Name\"");
            }
            if (dealerNoColumn == -1) {
                System.out.println("Could not find header that contains \"Dealer No\"");
            }
            System.out.println("Make sure that csv file contains these headers and are labeled correctly.");
            System.exit(1);
        }

        for (String id : parentMap.keySet()) {
            if (resultsMap.containsKey(id)) {
                String[] parentRow = parentMap.get(id);
                for (String[] resultRow : resultsMap.get(id)) {
                    toWrite.add(new String[]{parentRow[urlColumn], parentRow[dealerNoColumn],
                            parentRow[dealerNameColumn], parentRow[dealerCityColumn], resultRow[1], resultRow[2]});
                }
            } else {
                String[] parentRow = parentMap.get(id);
                boolean doesExist = false;
                for (String[] testIfEntryExists : toWrite) {
                    if (testIfEntryExists[1].equalsIgnoreCase(parentRow[dealerNoColumn])) {
                        doesExist = true;
                    }
                }
                if (doesExist == false) {
                    //Yahoo Local Addition.
                    if (isYahooLocal) {
                        String[] head = parentMap.get(id);
                        if (!head[yahooLocalNameColumn].isEmpty() && !head[yahooLocalLinkColumn].isEmpty()) {
                            toWrite.add(new String[]{parentRow[urlColumn], parentRow[dealerNoColumn],
                                    parentRow[dealerNameColumn], parentRow[dealerCityColumn],
                                    head[yahooLocalNameColumn], head[yahooLocalLinkColumn]});
                            doesExist = true;
                        }
                    }
                    // end
                    if (!doesExist) {
                        toWrite.add(new String[]{parentRow[urlColumn], parentRow[dealerNoColumn],
                                parentRow[dealerNameColumn], parentRow[dealerCityColumn], "NA", "NA"});
                    }
                }
            }
        }


        String folderPath = path.substring(0,path.lastIndexOf("\\")+1);
        String fileName = path.substring(path.lastIndexOf("\\")+1);
        csvUtils.writeCSV(folderPath + "Organized" + fileName, toWrite);

    }

    /*   Processes the results to try to find the real listing.

        @param results An array of the results as represented by an array containing
            the name and link of the entry. {name, link}
        @return A map of the results as represented by an array containing
            the name and link of the entry, and the integer representing its score
     */
    public Map<String[], Integer> processResults(String name, String city,
                                                 String dealerType, ArrayList<String[]> results) {

        Map<String[], Integer> resultsMap = new HashMap<>();
        String[] nameWords = name.split(" ");

        for (String[] result : results) {
            int hitPoints = 0;

            //Lower score of duplicate links
            for (String[] testingLinks: resultsMap.keySet()) {
                if (testingLinks[1].contains(result[1]) || result[1].contains(testingLinks[1])) {
                    hitPoints -= 2;
                }
            }

            String resultName = result[0];

            if (resultName.equalsIgnoreCase(name)) {
                hitPoints += 10;
            }

            if (resultName.toLowerCase().contains(name.toLowerCase())) {
                hitPoints += 5;
            }

            for (String word : nameWords) {
                if (resultName.toLowerCase().contains(word.toLowerCase())) {
                    hitPoints++;
                }
            }

            for (String resultWord:resultName.split(" ")) {
                for (String dealerTypeListEntry:dealerTypes) {

                    if (resultWord.equalsIgnoreCase(dealerTypeListEntry) &&
                            dealerTypeListEntry.equalsIgnoreCase(dealerType)) {
                        hitPoints++;
                    } else if (resultWord.equalsIgnoreCase(dealerTypeListEntry)) {
                        hitPoints -= 5;
                    }
                }
            }

            if (resultName.toLowerCase().contains(city)) {
                hitPoints -= city.split(" ").length;
            }
            resultsMap.put(result, hitPoints);
        }

        return resultsMap;
    }
}
