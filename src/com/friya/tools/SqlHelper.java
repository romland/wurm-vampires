package com.friya.tools;

import java.util.ArrayList;
import java.util.Scanner;

public class SqlHelper {

	static public ArrayList<String> getStatementsFromBatch(String sqlBatch)
	{
		ArrayList<String> ret = new ArrayList<String>(); 
		
		Scanner s = new Scanner(sqlBatch);
		s.useDelimiter("/\\*[\\s\\S]*?\\*/|--[^\\r\\n]*");
		
		try {
			StringBuffer currentStatement = new StringBuffer();
	
			while (s.hasNext()) {
				String line = s.next();
				
				if (line.startsWith("/*!") && line.endsWith("*/")) {
					int i = line.indexOf(' ');
					line = line.substring(i + 1, line.length() - " */".length());
				}
				
				if (line.trim().length() > 0) {
					currentStatement.append(line);
	
					if(line.contains(";")) {
						String[] tmp = currentStatement.toString().split(";");
						for(String ln : tmp) {
							if(ln.trim().length() == 0) {
								continue;
							}
							ret.add(ln);
						}
						currentStatement.setLength(0);
					}
							
				}
			}
		} finally {
			s.close();
		}
		
		return ret;
	}

}
