package com.company;

import java.io.File;
import java.util.*;

/**
 * @author Nalydmerc@gmail.com
 *
 *         3Birds Link/Result processor.
 *
 *         This program takes web scraper search result data in the form of a single or pair of CSVs.
 *         Given the name of a Car Dealership and a list of search results, this program scores each search result based
 *         on how closely it matches the name of the dealership in an attempt to pick which search result is the
 *         dealerships listing or page.
 */

public class ResultScorer {

    private static String[] dealerTypes = {"Honda", "Acura", "Toyota", "Ford", "Hyundai", "Dodge", "Chevrolet", "Chrysler", "Jeep", "Lexus", "Nissan", "Fiat", "Lincoln", "Mazda", "Infinity", "Jaguar", "Bently"};
    private static String[] blacklist = {"bbb.org/", "foursquare.com/", "yelp.com/", "yellowpages.com/", "yellowpages.ca/", "yelp.ca/", "twitter.com/", "instagram.com/", "cars.com/", "CitySearch.com/", "edmunds.com/", "facebook.com/", "yahoo.com/", "youtube.com/", "dealerrater.com/", "dealerrater.ca", "autotrader.com/", "autotrader.ca", "autocatch.com/", "wheels.com/", "unhaggle.com/", "oodle.com/", "monsterauto.ca/", "ourbis.ca/", "canpages.ca/", "goldbook.ca/"};
    private static String[] veryNegativeWords = {" used", " find", " for sale"};
    private static String[] possibleExtensions = {"Results", "Links", "dealers"};
    private int nameIndex = -1;
    private int resultNameIndex = -1;
    private int resultLinkIndex = -1;
    private CSV organizedCSV;

    /**
     * organizeCSV:
     * Pieces together two CSVs based on the output from Visual Web Ripper. When Visual Web Ripper exports Data,
     * heavily depending on how the data collector set up the template, VWR can export it's tables as separate
     * CSVs. When it exports, it gives each value a Primary Key to tie the values in the CSV to each other, which
     * this method uses to combine them. Imagine the two CSVs as two Database tables with a one to many
     * relationship. For every row in the main CSV, there are many results. This method combines them all into one
     * table. If your data is in two separate CSVs, you'll need to use this.
     *
     * The method sets the resulting combined CSV as this.organizedCSV. After using this, you can work with
     * this.organizedCSV
     *
     * @param mainCSVfile      contains one entry for every search. Headers must contain: ID (must be the first column.
     *                         VWR adds this automatically), Dealer No, Dealer Name
     * @param secondaryCSVfile contains multiple results for every search. Headers must contain: ID (must be the first
     *                         column. VWR adds this automatically), Result, Link
     */
    private void organizeCSV(File mainCSVfile, File secondaryCSVfile) {

        //Raw CSVs
        CSV mainCSV = new CSV(mainCSVfile);
        CSV secondaryCSV = new CSV(secondaryCSVfile);
        CSV toWrite = CSV.createNew(mainCSV.createNewFileWithPrefix("Organized"));

        ArrayList<String> headers = new ArrayList<>(Arrays.asList(mainCSV.getHeaders()));
        toWrite.setHeaders(mainCSV.getHeaders());
        toWrite.addHeader("Result");
        toWrite.addHeader("Link");

        //Make sure the CSVs contain the correct headers.
        resultNameIndex = secondaryCSV.getHeaderIndex("name");
        resultLinkIndex = secondaryCSV.getHeaderIndex("link");

        if (resultNameIndex == -1 || resultLinkIndex == -1) {
            System.out.println("Error: Could not find column numbers from headers.");
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
        Scanner in = new Scanner(System.in);

        //We begin by getting the main CSV with our data.
        do {
            String path = CSVUtils.requestPath("Enter main file path: ");
            mainCSVfile = new File(path);
            if (!mainCSVfile.exists()) {
                System.out.println("Error: That CSV does not exist. Double check your file path.");
            }
        } while (!mainCSVfile.exists());

        //Organize the CSVs First. We need to stitch the two CSVs together to work with them
        //(If they are indeed separated)
        System.out.println("Is your data separated into two CSVs, like output from Visual Web Ripper, needing to be" + " organized? (yes/no): ");
        String input = in.nextLine().toLowerCase();
        if (input.contains("yes") || input.contains("es") || input.contains("ye") || input.equalsIgnoreCase("y")) {

            //The user indicated that there are two CSVs, so our data needs to be organized. We'll look for some
            //common filenames the secondaryCSV might be named, and ask the user for the file path if we can't
            String mainCSVAbPath = mainCSVfile.getAbsolutePath();
            String folderPath = mainCSVAbPath.substring(0, mainCSVAbPath.lastIndexOf("\\") + 1);
            String fileNameWithoutExtension = mainCSVAbPath.substring(mainCSVAbPath.lastIndexOf("\\") + 1, mainCSVAbPath.length() - 4);
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

            System.out.println("[ResultSorter] Organizing CSV file...");
            organizeCSV(mainCSVfile, secondaryCSVfile);
            System.out.println("[ResultSorter] CSV Organized.");
        } else {
            //There is no need for organization, so we read the single given CSV into this.organizedCSV to work with.
            organizedCSV = new CSV(mainCSVfile);

            //Make sure the CSV contains the correct headers.
            resultNameIndex = organizedCSV.getHeaderIndex("result");
            resultLinkIndex = organizedCSV.getHeaderIndex("link");
            if (resultNameIndex == -1 || resultLinkIndex == -1) {
                System.out.println("Error: Could not find column numbers from headers.");
                if (resultNameIndex == -1) {
                    System.out.println("Could not find header in the results CSV that contains \"Result\"");
                }
                if (resultLinkIndex == -1) {
                    System.out.println("Could not find header in the results CSV that contains \"Link\"");
                }
                System.out.println("Make sure that csv file contains these headers and are labeled correctly.");
                System.exit(1);
            }
        }

        /*
         * From this point forward the CSVs are now organized. All of our data is contained in the organizedCSV.
         */

        //Get index of Client Name
        String[] headers = organizedCSV.getHeaders();
        for (int i = 1; i < headers.length; i++) {
            String head = headers[i];
            if (head.toLowerCase().contains("clientname") || head.toLowerCase().contains("client name")) {
                this.nameIndex = i;
            }
        }

        if (nameIndex == -1) {
            System.out.println("Could not find header in the parent CSV that contains \"Client Name\"");
            System.exit(1);
        }

        //Blacklist
        System.out.println("=========\n\n=========");
        System.out.print("Enabling Blacklist will drop common social media and review sites.\n" +
                "This reduces false positives if you are searching for unique URLs." +
                "\n\nUse Blacklist? (Y/N): ");
        input = in.nextLine().toLowerCase();
        boolean useBL = false;
        if (input.contains("yes") || input.contains("es") || input.contains("ye") || input.equalsIgnoreCase("y")) {
            useBL = true;
        }

        //Searching results on a specific site
        System.out.println("=========\n\n=========");
        System.out.println("If you are searching for pages containing a specific string. With this you can drop " +
                "\nresults that do not contain a given string in it's url. For example, entering \"facebook\" will " +
                "\nonly produce results with facebook in the url." +
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
        System.out.println("=========\n\n=========");
        System.out.println("Scoring the URL as an extra measure can increase accuracy when searching for unique URLS," +
                "\nsuch as dealer websites. If you're only processing results from a single website (e.g. facebook)," +
                "\nyou will want to turn this off." +
                "\n\nScore URLS? (Y/N)");
        input = in.nextLine().toLowerCase();
        boolean scoreURLs = false;
        if (input.contains("yes") || input.contains("es") || input.contains("ye") || input.equalsIgnoreCase("y")) {
            scoreURLs = true;
        }

        /*
            We have what we need. We dispose of the now useless user, and start working.
         */
        System.out.println("[ResultSorter] Iterating through results...");
        int i = 1;

        CSV processedOrganizedCSV = CSV.createNew(organizedCSV.createNewFileWithPrefix("Processed"));
        processedOrganizedCSV.setHeaders(organizedCSV.getHeaders());
        processedOrganizedCSV.addHeader("Confidence");

        while (i < organizedCSV.getContent().size() - 1) {

            String[] line = organizedCSV.getContent().get(i);
            String id = line[0];
            String name = line[nameIndex];

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
            }

            //Score Results
            Map<String[], Integer> resultsMap = this.scoreResults(name, results, scoreURLs);
            int highestScore = 0;
            for (int score : resultsMap.values()) {
                if (score > highestScore) {
                    highestScore = score;
                }
            }

            //Add to processed CSV
            for (Map.Entry<String[], Integer> entry : resultsMap.entrySet()) {
                if (entry.getValue() >= highestScore - 1) {

                    ArrayList<String> rowToAdd = new ArrayList<>(Arrays.asList(organizedCSV.getContent().get(i)));

                    //Calculate human readable confidence value.
                    //Ha, "Human readable." No-one really knows what this means.
                    double score = entry.getValue();
                    double max = name.split(" ").length + 10;
                    double percent = (score / max) * 100;
                    percent *= 10;
                    percent = Math.round(percent);
                    percent /= 10;
                    rowToAdd.add(percent + "%");

                    //Add to CSV.
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
     * @param url full String url including ".com/"
     * @return true/false if the url is blacklisted
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
     * @param results An ArrayList of the results as represented by an array containing
     *                the name and link of the entry. {name, link}
     * @param name    of dealership; This is what we're trying to find the closes match to.
     * @return A map of the results as represented by an array containing
     * the name and link of the entry, and the integer representing its score
     */

    public Map<String[], Integer> scoreResults(String name, ArrayList<String[]> results, boolean scoreURLs) {

        Map<String[], Integer> resultsMap = new HashMap<>();
        String[] nameWords = name.split(" ");

        //Find makes in the name so we can match them.
        //"Ey, you sell Chevys or Lamborghinis?"
        ArrayList<String> currentDealerTypes = new ArrayList<>();
        for (String dealer : dealerTypes) {
            if (name.contains(dealer)) {
                currentDealerTypes.add(dealer.toLowerCase());
            }
        }

        for (String[] result : results) {
            int hitPoints = 0;

            //Lower score of duplicate links
            for (String[] testingLinks : resultsMap.keySet()) {
                if (testingLinks[1].contains(result[1]) || result[1].contains(testingLinks[1])) {
                    hitPoints -= 3;
                }
            }

            String resultName = result[0];

            //10/10 Result name, jackpot, you got it, add 10 points.
            if (resultName.toLowerCase().equalsIgnoreCase(name.toLowerCase())) {
                hitPoints += 10;
            }

            //Almost an exact match. The result name *contains* the exact name of the dealer.
            if (resultName.toLowerCase().contains(name.toLowerCase())) {
                hitPoints += 5;
            }

            for (String word : nameWords) {
                if (resultName.toLowerCase().contains(word.toLowerCase())) {
                    hitPoints++;
                }
            }

            for (String resultWord : resultName.split(" ")) {
                for (String dealerTypeListEntry : dealerTypes) {
                    //Add a point if the make matches the dealer. Subract five points otherwise.
                    //PS: Learn how to spell subtract, Dylan.
                    if (resultWord.equalsIgnoreCase(dealerTypeListEntry) && currentDealerTypes.contains(dealerTypeListEntry.toLowerCase())) {
                        hitPoints += 1;
                    } else if (resultWord.equalsIgnoreCase(dealerTypeListEntry)) {
                        hitPoints -= 5;
                    }
                }
            }

            //I mean when someone cusses at you, you throw them out, right?
            //JK, we know if the link contains any of these names, it's out.
            //I.E. We're a car dealer, not the Better Business Bureau.
            for (String word : veryNegativeWords) {
                if (resultName.toLowerCase().contains(word.toLowerCase())) {
                    hitPoints -= 20;
                }
            }

            //Remove point if the city is contained in the name for some reason. No longer used.
            //if (resultName.toLowerCase().contains(city)) {
            //    hitPoints -= city.split(" ").length;
            //}

            //Score URL based on everything after "www." and before ".com"
            if (scoreURLs) {

                //Format the URL String first so we can work with it more easily.
                //We're matching only main url, nothing after the first '/'.
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

                //SuperDirectMatch, 10/10 URL naming, +10 Points, Level Up, Advance to Go, Collect $200.
                if (resultURL.contains(scoreAgainstName)) {
                    hitPoints += 10;
                }

                //"I'll take a point for a name for 200, Alex."
                //+1 Point for every name word contained in the URL.
                for (String word : nameWords) {
                    if (resultURL.contains(word.toLowerCase())) {
                        hitPoints++;
                    }
                }

                //Add point for every make that matches the name
                //We sell Chevys.
                for (String make : currentDealerTypes) {
                    if (resultURL.contains(make.toLowerCase())) {
                        hitPoints++;
                    }
                }

                //Remove for every make that does not match the name.
                //I said we sell Chevys not Lamborghinis, kid.
                for (String make : dealerTypes) {
                    if (resultURL.contains(make.toLowerCase()) && !currentDealerTypes.contains(make.toLowerCase())) {
                        hitPoints -= 2;
                    }
                }
            }

            //Check if name is blank, assign NA and 0 score accordingly.
            if (name.equals("")) {
                hitPoints = 0;
                name = "NA";
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
