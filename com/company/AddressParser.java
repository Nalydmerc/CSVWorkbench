package com.company;

import java.io.*;

/**
 * @author Nalydmerc
 *         Created 1/4/2016
 *         <p>
 *         So I'm trying to use the pythonCode usaddress library when I already have this written in Java.
 *         No, it's not *great* practice. But it's a lot less work at the moment, and I'll do my best to code it as
 *         well as possible.
 *         <p>
 *         //TODO NOT FINISHED YET. This was put on the backburner. I'll work on it eventually.
 */
public class AddressParser {

    private String pythonCode = "from collections import OrderedDict\n" +
            "import usaddress\n" +
            "\n" +
            "\n" +
            "def parse(address_string):\n" +
            "    addict = usaddress.parse(address_string)\n" +
            "    for item in addict:\n" +
            "        print item\n" +
            "\n" +
            "address = \"\"\n" +
            "while address is not \"exit\":\n" +
            "    address = raw_input(\"enter address:\")\n" +
            "    parse(address)";

    public void parseAddresses() {

        try {
            /*
            File pythonProgram = File.createTempFile("pythonCode", ".py");
            String path = pythonProgram.getCanonicalPath();
            System.out.print(path);
            PrintWriter pw = new PrintWriter(path);
            pw.print(pythonCode);
            pw.close();
            */

            Process python = Runtime.getRuntime().exec("echo ");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(python.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(python.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(python.getErrorStream()));

            String testAddress = "3201 Bennett Rd. Oak City NC 27857";
            System.out.println("asdf");
            writer.write(testAddress);
            System.out.println("asdf");

            String line = "1";
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }

            while ((line = error.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Required path of pythonCode program.
     */

    public enum AddressElement {
        AddressNumber,
        StreetNamePreType,
        StreetName,
        StreetNamePostType,
        OccupancyType,
        OccupancyIdentifier,
        PlaceName,
        StateName,
        ZipCode
    }

}
