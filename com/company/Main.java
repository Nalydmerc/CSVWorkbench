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
 *
 * @author Nalydmerc@gmail.com
 *
 * This is a workbench for CSV (Comma Separated Value) files. This program treats the CSVs more like database tables,
 * and the order which rows are kept cannot necessarily be guaranteed.
 *
 * Also contained in this program is the Link Result Scorer. See ResultScorer class for more information.
 *
 * About CSV Files:
 *     CSV files are spreadsheets that are contained in a text file with values separated by commas.
 *     Data can be exported from Visual Web Ripper (Web scrapin gsoftware for which this program was originally
 *     designed to work with) and Microsoft Excell in the form of CSV files. Data in CSV Files can be easily
 *     be compared to spreadsheets (such Excell exports) or DataBase tables, with a header that describes what each
 *     column in a row means, and many rows of data where each row is a single entry.
 *
 *
 *
 * //TODO Make project into a library, seperate use from utilities.
 */

public class Main {


    public static void main(String[] args) {
        ResultScorer r = new ResultScorer();
        r.run();
    }

    public String[] findRegexIn(String text, String regexSubstring) {
        ArrayList<String> results = new ArrayList<>();
        Pattern reg = Pattern.compile(regexSubstring);
        Matcher matcher = reg.matcher(text);
        for (int i = 0; i < matcher.groupCount(); i++) {
            results.add(matcher.group(i));
        }
        return results.toArray(new String[results.size()]);
    }
}