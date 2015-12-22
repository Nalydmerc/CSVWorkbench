
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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CSVWorkbench
 * @author Nalydmerc
 *
 * A work area with CSV Utilities.
 * This is designed to be used on the fly. Code in main() will usually be for one time
 * CSV runs made for a specific purpose.
 *
 * For ResultScorer to run propperly, two CSV files are needed.
 *  Both CSVs must have a Unique ID as the first column. Visual Web Ripper does this by default.
 *  The main CSV must have columns labeled "Start URL", "Dealer No", "Dealer Name", "Dealer City"
 *  If you do not have the Dealer Number, you need to at least add a column with the "Dealer No" header for the
 *      program to run properly.
 *  //TODO Fix that.
 *  //TODO add method to list all files if no files are found containing "_"
 *  The secondary CSV must have column labeled "Name" and "Link"
 */

public class Main {

    public static String CurrentDealerType = "";
    private ResultScorer resultScorer = new ResultScorer(this);
    private CSVUtils csvUtils = new CSVUtils();

    public static void main(String[] args) {
        Main main = new Main();
        main.resultScorer.run();
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
}
