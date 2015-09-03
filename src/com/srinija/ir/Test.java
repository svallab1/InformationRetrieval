package com.srinija.ir;
import static java.lang.System.out;

import java.nio.file.Files;
import java.nio.file.Paths;
 
public class Test {
    public static void main(String[] args) throws Exception {
	out.println(new String(Files.readAllBytes(Paths.get("resources/bashscript.sh"))));
    }
}