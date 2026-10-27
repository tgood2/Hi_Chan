package com.hichan;

/**
 * production environmeny for driver entry point for the Hi_chan command line
 */

public class Main {
    /**
     * main direct deployment program execution route
     */
    public static void main(String[] args){
        try {
            HiChanCLI commandLine = new HiChanCLI();

            commandLine.run(args);
        } catch(Exception e) {
            /**
             * prevent leaking of internal stack trace codes
             * global fallback safety handler
             */
            System.err.println("System Error!");
            System.exit(2);
        }

    }
}