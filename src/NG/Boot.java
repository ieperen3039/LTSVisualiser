package NG;

import NG.Core.Main;
import NG.Tools.Logger;
import org.lwjgl.system.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Boots the Root
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Boot {
    public static void main(String[] argArray) throws Exception {
        List<String> args = new ArrayList<>(Arrays.asList(argArray));

        if (args.contains("-debug")) {
            Logger.setLoggingLevel(Logger.DEBUG);

        } else if (args.contains("-quiet")) {
            Logger.setLoggingLevel(Logger.ERROR);

        } else {
            Logger.setLoggingLevel(Logger.INFO);
        }

        String graphString;

        int lts = args.indexOf("-lts");
        if (lts != -1){
            StringBuilder str = new StringBuilder();
            File source = new File(args.get(lts + 1));
            Logger.DEBUG.print("Opening " + source.getCanonicalPath());

            Scanner scanner = new Scanner(source);
            while (scanner.hasNext()) str.append(scanner.nextLine()).append("\n");
            graphString = str.toString();

        } else {
            System.out.println("Insert Graph: ");
            Scanner scin = new Scanner(System.in);
            graphString = scin.next();
        }

        if (args.contains("-lwjgl-debug")) {
            Configuration.DEBUG.set(true);
        }

        new Main(graphString).root();
    }
}
