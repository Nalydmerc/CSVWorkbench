package com.company;

import java.io.File;
import java.util.*;

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
    private static String[] veryNegativeWords = {" used", " find", " for sale"}; //Make sure these are lowercase
    private static String[] possibleExtensions = {"Results","Links","dealers"};
    private int nameIndex = -1;
    private int cityIndex = -1;
    private int resultNameIndex = -1;
    private int resultLinkIndex = -1;
    private CSV organizedCSV;

    /**
     * Pieces together two CSVs based on the output from Visual Web Ripper. TODO Update this JavaDoc. It's old.
     *
     * @param mainCSVfile      contains one entry for every search. Headers: ID (must be first column), Dealer No, Dealer Name, Start URL
     * @param secondaryCSVfile contains multiple results for every search. headers: ID (must be first column), Name, Link
     * @return List of rows, one row for each result, which also contains information from the mainCSV about the search.
     * Column headers ordered: SearchURL, Dealer No, Search Name, Result, Link
     * @implNote There are special additions to handle yahoo local links, as they're a bit different. Additions are properly commented.
     */
    private void organizeCSV(File mainCSVfile, File secondaryCSVfile) {

        //Raw CSVs
        CSV mainCSV = new CSV(mainCSVfile);
        CSV secondaryCSV = new CSV(secondaryCSVfile);
        CSV toWrite = CSV.createNew(mainCSV.createNewFileWithPrefix("Organized"));

        ArrayList<String> headers = new ArrayList<>(Arrays.asList(mainCSV.getContent().get(0)));
        headers.add("Result");
        headers.add("Link");
        toWrite.add(headers.toArray(new String[headers.size()]));

        //Get indexes
        for (int i = 1; i < headers.size(); i++) {
            String head = headers.get(i);
            if (head.toLowerCase().contains("name")) {
                this.nameIndex = i;
            } else if (head.toLowerCase().contains("city")) {
                this.cityIndex = i;
            }
        }

        headers = new ArrayList<String>(Arrays.asList(secondaryCSV.getContent().get(0)));

        for (int i = 1; i < headers.size(); i++) {
            String head = headers.get(i);
            if (head.toLowerCase().contains("name")) {
                resultNameIndex = i;
            } else if (head.toLowerCase().contains("link")) {
                resultLinkIndex = i;
            }
        }

        if (nameIndex == -1 || cityIndex == -1 || resultNameIndex == -1 || resultLinkIndex == -1) {
            System.out.println("Error: Could not find column numbers from headers.");
            if (cityIndex == -1) {
                System.out.println("Could not find header in the parent CSV that contains \"City\"");
            }
            if (nameIndex == -1) {
                System.out.println("Could not find header in the parent CSV that contains \"Dealer Name\"");
            }
            if (resultNameIndex == -1) {
                System.out.println("Could not find header in the results CSV that contains \"Name\"");
            }
            if (resultLinkIndex == -1) {
                System.out.println("Could not find header in the results CSV that contains \"Link\"");
            }
            System.out.println("Make sure that csv file contains these headers and are labeled correctly.");
            System.exit(1);
        }

        //ID, other information including name and city
        HashMap<String, String[]> parentMap = new HashMap<>();

        //ID, ArrayList<Result Name, Result Link>
        HashMap<String, ArrayList<String[]>> resultsMap = new HashMap<>();

        //Create parent ID map
        for (String[] row : mainCSV.getContent()) {
            parentMap.put(row[0], row);
        }


        //Populate resultsMap
        for (String[] row : secondaryCSV.getContent()) {
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

        ArrayList<String> presentIDs = new ArrayList<>();

        for (String id : parentMap.keySet()) {
            if (resultsMap.containsKey(id)) {
                String[] parentRow = parentMap.get(id);
                for (String[] resultRow : resultsMap.get(id)) {
                    presentIDs.add(id);
                    ArrayList<String> rl = new ArrayList<>(Arrays.asList(parentRow));
                    rl.add(resultRow[resultNameIndex]);
                    rl.add(resultRow[resultLinkIndex]);
                    toWrite.add(rl.toArray(new String[rl.size()]));
                }
            } else {
                String[] parentRow = parentMap.get(id);
                if (!presentIDs.contains(id)) {
                    ArrayList<String> rl = new ArrayList<>(Arrays.asList(parentRow));
                    rl.add("NA");
                    rl.add("NA");
                    toWrite.add(rl.toArray(new String[rl.size()]));
                }
            }
        }

        resultNameIndex = toWrite.getContent().get(0).length - 2;
        resultLinkIndex = toWrite.getContent().get(0).length - 1;

        toWrite.dump();
        this.organizedCSV = toWrite;
    }

    /**
     * Starts the ResultScorer.
     */
    public void run() {

        File mainCSVfile;
        File secondaryCSVfile = null;

        do {
            String path = CSVUtils.requestPath("Enter main file path: ");
            mainCSVfile = new File(path);
            if (!mainCSVfile.exists()) {
                System.out.println("Error: That CSV does not exist. Double check your file path.");
            }
        } while (!mainCSVfile.exists());

        //Secondary CSV
        String mainCSVAbPath = mainCSVfile.getAbsolutePath();
        String folderPath = mainCSVAbPath.substring(0, mainCSVAbPath.lastIndexOf("\\")+1);
        String fileNameWithoutExtension = mainCSVAbPath.substring(mainCSVAbPath.lastIndexOf("\\")+1, mainCSVAbPath.length()-4);
        Scanner in = new Scanner(System.in);
        boolean found = false;

        for (String testE : possibleExtensions) {
            secondaryCSVfile = new File(folderPath + fileNameWithoutExtension + "_" + testE + ".csv");
            if (secondaryCSVfile.exists()) {
                found = true;
                break;
            }
        }

        if (found) {
            System.out.println("Found results file.");
        } else {
            secondaryCSVfile = this.requestResultsFile(folderPath);
        }

        //Dealer type
        System.out.println("Enter Dealer Type. Make sure to double check your spelling:");
        String currentDealerType = in.nextLine();

        //Blacklist
        System.out.print("Enabling Blacklist will drop common social media and review sites.\n" +
                "This reduces false positives if you are searching for unique URLs." +
                "\n\nUse Blacklist? (Y/N): ");
        String input = in.nextLine().toLowerCase();
        boolean useBL = false;
        if (input.contains("yes") || input.contains("es") || input.contains("ye") || input.equalsIgnoreCase("y")) {
            useBL = true;
        }

        //Searching results on a specific site
        System.out.println("If you are searching for pages from a specific site (e.g. facebook), you can drop results" +
                "\nthat do not contain a given string in it's url. For example, entering \"facebook\" will only" +
                "\nproduce results with facebook in the url." +
                "\n\nSpecific Site Search? (Y/N)");
        input = in.nextLine().toLowerCase();
        boolean specificWebsiteSearch = false;
        String specificWebsite = "";
        if (input.contains("yes") || input.contains("es") || input.contains("ye") || input.equalsIgnoreCase("y")) {
            specificWebsiteSearch = true;
            System.out.println("Enter string to search for in URL: ");
            specificWebsite = in.next().toLowerCase();
        }

        //URLscoring
        System.out.println("Scoring the URL as an extra measure can increase accuracy when searching for unique URLS," +
                "\nsuch as dealer websites. If you're only processing results from a single website (e.g. facebook)," +
                "\nyou will want to turn this off." +
                "\n\nScore URLS? (Y/N)");
        input = in.nextLine().toLowerCase();
        boolean scoreURLs = false;
        if (input.contains("yes") || input.contains("es") || input.contains("ye") || input.equalsIgnoreCase("y")) {
            scoreURLs = true;
        }
        //End user input

        //Organize
        System.out.println("[ResultSorter] Organizing CSV file...");
        organizeCSV(mainCSVfile, secondaryCSVfile);
        System.out.println("[ResultSorter] CSV Organized.");

        //Iterate through results
        System.out.println("[ResultSorter] Iterating through results...");
        int i = 1;

        String newPath = folderPath + "Processed" + fileNameWithoutExtension + ".csv";
        File f = new File(newPath);
        CSV processedOrganizedCSV = CSV.createNew(f);
        processedOrganizedCSV.add(organizedCSV.getContent().get(0));

        while (i < organizedCSV.getContent().size() - 1) {

            String[] line = organizedCSV.getContent().get(i);
            String id = line[0];
            String name = line[nameIndex];
            String city = line[cityIndex];

            ArrayList<String[]> results = new ArrayList<>();

            int resultIndex = 0;
            if (i + resultIndex < organizedCSV.getContent().size()) {
                while (i + resultIndex < organizedCSV.getContent().size() && organizedCSV.getContent().get(i + resultIndex)[0].equalsIgnoreCase(id)) {
                    String[] result = {organizedCSV.getContent().get(i + resultIndex)[resultNameIndex], organizedCSV.getContent().get(i + resultIndex)[resultLinkIndex]};
                    //Check Blacklist
                    if (useBL) {
                        if (!isBlacklisted(result[1])) {
                            //Check Specific Website String
                            if (!specificWebsiteSearch || result[1].contains(specificWebsite)) {
                                results.add(result);
                            }
                        }
                    } else {
                        //Check Specific Website String
                        if (!specificWebsiteSearch || result[1].contains(specificWebsite)) {
                            results.add(result);
                        }
                    }
                    resultIndex++;
                }
            } else {
                String[] result = {organizedCSV.getContent().get(i + resultIndex)[resultNameIndex],
                        organizedCSV.getContent().get(i + resultIndex)[resultLinkIndex]};
                //Check Blacklist
                if (useBL) {
                    if (!isBlacklisted(result[1])) {
                        //Check Specific Website String
                        if (!specificWebsiteSearch || result[1].contains(specificWebsite)) {
                            results.add(result);
                        }
                    }
                } else {
                    //Check Specific Website String
                    if (!specificWebsiteSearch || result[1].contains(specificWebsite)) {
                        results.add(result);
                    }
                }
            }

            //Score Results
            Map<String[], Integer> resultsMap = this.scoreResults(name, city, currentDealerType, results, scoreURLs);
            int highestScore = 0;
            for (int score : resultsMap.values()) {
                if (score > highestScore) {
                    highestScore = score;
                }
            }

            //Add to processed CSV
            for (Map.Entry<String[], Integer> entry : resultsMap.entrySet()) {
                if (entry.getValue() >= highestScore-1) {

                    ArrayList<String> rowToAdd = new ArrayList<>(Arrays.asList(organizedCSV.getContent().get(i)));
                    rowToAdd.set(rowToAdd.size() - 2, entry.getKey()[0]);
                    rowToAdd.set(rowToAdd.size() - 1, entry.getKey()[1]);
                    rowToAdd.add(entry.getValue().toString());
                    processedOrganizedCSV.add(rowToAdd.toArray(new String[rowToAdd.size()]));

                }
            }

            i += resultIndex;
        }

        processedOrganizedCSV.dump();
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

            if (resultName.toLowerCase().equalsIgnoreCase(name.toLowerCase())) {
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

            for (String word : veryNegativeWords) {
                if (resultName.toLowerCase().contains(word)) {
                    hitPoints -= 20;
                }
            }

            //if (resultName.toLowerCase().contains(city)) {
            //    hitPoints -= city.split(" ").length;
            //}

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
