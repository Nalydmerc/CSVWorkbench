package com.company;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ResultScorer {

    private static String[] dealerTypes = {
            "Honda", "Acura", "Toyota", "Ford", "Hyundai", "Dodge", "Chevrolet","Chrysler",
            "Jeep", "Lexus", "Nissan", "Fiat", "Lincoln", "Mazda", "Infinity", "Jaguar", "Bently"};
    private static String[] blacklist = {
            "bbb.org/", "foursquare.com/", "yelp.com/", "yellowpages.com/", "yellowpages.ca/", "yelp.ca/",
            "twitter.com/", "instagram.com/", "cars.com/", "CitySearch.com/", "edmunds.com/", "facebook.com/",
            "yahoo.com/", "youtube.com/", "dealerrater.com/", "dealerrater.ca", "autotrader.com/", "autotrader.ca",
            "autocatch.com/", "wheels.com/", "unhaggle.com/", "oodle.com/", "monsterauto.ca/", "ourbis.ca/",
            "canpages.ca/", "goldbook.ca/"};
    private static String[] possibleExtensions = {"Results","Links","dealers"};
    private CSVUtils csvUtils = new CSVUtils();
    private Main main;

    public ResultScorer(Main main) {
        this.main = main;
    }

    /**
     * Pieces together two CSVs based on the output from Visual Web Ripper.
     *
     * @param mainCSV      contains one entry for every search. Headers: ID (must be first column), Dealer No, Dealer Name, Start URL
     * @param secondaryCSV contains multiple results for every search. headers: ID (must be first column), Name, Link
     * @return List of rows, one row for each result, which also contains information from the mainCSV about the search.
     * Column headers ordered: SearchURL, Dealer No, Search Name, Result, Link
     * @implNote There are special additions to handle yahoo local links, as they're a bit different. Additions are properly commented.
     */
    private ArrayList<String[]> organizeCSV(File mainCSV, File secondaryCSV) {

        ArrayList<String[]> parsedMainCSV = CSVUtils.getCSV(mainCSV.getPath());
        ArrayList<String[]> parsedSecondaryCSV = CSVUtils.getCSV(secondaryCSV.getPath());
        ArrayList<String[]> toWrite = new ArrayList<>();

        HashMap<String, String[]> parentMap = new HashMap<>();
        HashMap<String, ArrayList<String[]>> resultsMap = new HashMap<>();

        for (String[] row : parsedMainCSV) {
            parentMap.put(row[0], row);
        }

        for (String[] row : parsedSecondaryCSV) {
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
        String[] headers = parsedMainCSV.get(0);

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
                if (!doesExist) {
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


        String path = mainCSV.getPath();
        String folderPath = path.substring(0,path.lastIndexOf("\\")+1);
        String fileName = path.substring(path.lastIndexOf("\\")+1);
        CSVUtils.writeCSV(folderPath + "Organized" + fileName, toWrite);
        return toWrite;
    }

    /**
     * Starts the ResultScorer.
     */
    public void run() {

        File mainCSV;
        File secondaryCSV = null;

        do {
            String path = CSVUtils.requestPath("Enter main file path: ");
            mainCSV = new File(path);
            if (!mainCSV.exists()) {
                System.out.println("Error: That CSV does not exist. Double check your file path.");
            }
        } while(!mainCSV.exists());

        //Secondary CSV
        String mainCSVAbPath = mainCSV.getAbsolutePath();
        String folderPath = mainCSVAbPath.substring(0, mainCSVAbPath.lastIndexOf("\\")+1);
        String fileNameWithoutExtension = mainCSVAbPath.substring(mainCSVAbPath.lastIndexOf("\\")+1, mainCSVAbPath.length()-4);
        Scanner in = new Scanner(System.in);
        boolean found = false;

        for (String testE : possibleExtensions) {
            secondaryCSV = new File(folderPath + fileNameWithoutExtension + "_" + testE + ".csv");
            if (secondaryCSV.exists()) {
                found = true;
                break;
            }
        }

        if (found) {
            System.out.println("Found results file.");
        } else {
            secondaryCSV = this.requestResultsFile(folderPath);
        }

        //Dealer type
        System.out.println("Enter Dealer Type. Make sure to double check your spelling:");
        String currentDealerType = in.nextLine();

        //Blacklist
        System.out.print("Enabling Blacklist will drop common social media and review sites.\n" +
                "This reduces false positives if you are searching for unique URLs." +
                "\nUse Blacklist? (Y/N): ");
        String input = in.nextLine().toLowerCase();
        boolean useBL = false;
        if (input.contains("yes") || input.contains("es") || input.contains("ye") || input.equalsIgnoreCase("y")) {
            useBL = true;
        }

        //URLscoring
        System.out.println("Scoring the URL as an extra measure can increase accuracy when searching for unique URLS," +
                "\nsuch as dealer websites. If you're only processing results from a single website, you might want to" +
                "\nturn this off." +
                "\nScore URLS? (Y/N)");
        input = in.nextLine().toLowerCase();
        boolean scoreURLs = false;
        if (input.contains("yes") || input.contains("es") || input.contains("ye") || input.equalsIgnoreCase("y")) {
            scoreURLs = true;
        }

        //End user input

        System.out.println("[ResultSorter] Organizing CSV file...");
        ArrayList<String[]> parsedOrganizedCSV = this.organizeCSV(mainCSV, secondaryCSV);
        System.out.println("[ResultSorter] CSV Organized.");

        ArrayList<String[]> processedOrganizedCSV = new ArrayList<>();
        processedOrganizedCSV.add(new String[]{"Search URL","Dealer No","Search Name","Result","Link"});

        System.out.println("[ResultSorter] Iterating through results...");

        int i = 1;
        while (i < parsedOrganizedCSV.size()-1) {

            String[] line = parsedOrganizedCSV.get(i);
            String startUrl = line[0];
            String dealerNo = line[1];
            String name = line[2];
            String city = line[3];

            ArrayList<String[]> results = new ArrayList<>();

            int resultIndex = 0;
            if (i + resultIndex < parsedOrganizedCSV.size()) {
                while (parsedOrganizedCSV.get(i+resultIndex)[0].equalsIgnoreCase(startUrl)) {
                    String[] result = {parsedOrganizedCSV.get(i + resultIndex)[4], parsedOrganizedCSV.get(i + resultIndex)[5]};
                    //Check Blacklist
                    if (useBL) {
                        if (!isBlacklisted(result[1])) {
                            results.add(result);
                        }
                    } else {
                        results.add(result);
                    }
                    if (i + resultIndex + 1 >= parsedOrganizedCSV.size()) {
                        break;
                    }
                    resultIndex++;
                }
            } else {
                String[] result = {parsedOrganizedCSV.get(i + resultIndex)[4], parsedOrganizedCSV.get(i + resultIndex)[5]};
                //Check Blacklist
                if (useBL) {
                    if (!isBlacklisted(result[1])) {
                        results.add(result);
                    }
                } else {
                    results.add(result);
                }

                resultIndex++;
            }

            Map<String[], Integer> resultsMap = this.scoreResults(name, city, currentDealerType, results, scoreURLs);



            int highestScore = 0;

            for (int score : resultsMap.values()) {
                if (score > highestScore) {
                    highestScore = score;
                }
            }

            for (Map.Entry<String[], Integer> entry : resultsMap.entrySet()) {
                if (entry.getValue() >= highestScore-1) {
                    String[] toAdd = {startUrl, dealerNo, name, entry.getKey()[0], entry.getKey()[1]};
                    processedOrganizedCSV.add(toAdd);
                }
            }

            i += resultIndex;
        }

        CSVUtils.writeCSV(folderPath + "Processed" + fileNameWithoutExtension + ".csv", processedOrganizedCSV);
    }

    /**
     * Check if url is on blacklist.
     *
     * @param url
     */
    private boolean isBlacklisted(String url) {
        //Check blacklist
        boolean isBlacklisted = false;
        for (String site : blacklist) {
            if (url.toLowerCase().contains(site)) {
                isBlacklisted = true;
            }
        }
        return isBlacklisted;
    }

    /**
     * Processes the results to find the correct listing.
     *
     * @param results An array of the results as represented by an array containing
     *      the name and link of the entry. {name, link}
     * @param name of dealership; used in the scoring process
     * @param city of dealership; used in the scoring process
     * @param dealerType used in the scoring process
     * @return A map of the results as represented by an array containing
     *      the name and link of the entry, and the integer representing its score
     */

    public Map<String[], Integer> scoreResults(String name, String city, String dealerType,
                                               ArrayList<String[]> results, boolean scoreURLs) {

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

            //Score URL
            if (scoreURLs) {

                //Format strings first
                String resultURL = result[1].toLowerCase();
                if (resultURL.startsWith("http://")) {
                    resultURL = resultURL.substring(7);
                } else if (resultURL.startsWith("https://")) {
                    resultURL = resultURL.substring(8);
                }
                if (resultURL.contains("/")) {
                    resultURL = resultURL.substring(0, resultURL.indexOf("/"));
                }
                String scoreAgainstName = name.replace(" ", "");
                scoreAgainstName = scoreAgainstName.toLowerCase();

                //Begin scoring
                if (resultURL.contains(scoreAgainstName)) {
                    hitPoints += 10;
                }

                for (String word : nameWords) {
                    if (resultURL.contains(word.toLowerCase())) {
                        hitPoints++;
                    }
                }

                if (resultURL.contains(dealerType.toLowerCase())) {
                    hitPoints++;
                }

                for (String type : dealerTypes) {
                    if (resultURL.contains(type.toLowerCase()) && !type.equalsIgnoreCase(dealerType)) {
                        hitPoints -= 3;
                    }
                }
            }
            resultsMap.put(result, hitPoints);
        }

        return resultsMap;
    }

    private File requestResultsFile(String folderPath) {
        Scanner in = new Scanner(System.in);
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        while (true) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].getName().contains("_")) {
                    System.out.println(i + ": " + listOfFiles[i].getName());
                }
            }
            try {
                System.out.println("Please choose results csv from list:");
                int fileNo = Integer.parseInt(in.nextLine());
                return listOfFiles[fileNo];
            } catch (NumberFormatException ex) {
                System.out.println("\nError: Input was not an integer. Enter the number of the entry from the list.\n");
            }
        }
    }
}
