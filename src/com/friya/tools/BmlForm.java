/**
 * Very basic helper to bring some sanity to BML forms (I wrote it years before it
 * made it into Wurm; could now likely be replaced with built-ins).
 */
package com.friya.tools;

import java.util.logging.Level;
import java.util.logging.Logger;


public class BmlForm
{
	private static Logger logger = Logger.getLogger(BmlForm.class.getName());
	private final StringBuffer buf = new StringBuffer();
	private static final String tabs = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";

	private int openBorders = 0;
	private int openCenters = 0;
	private int openVarrays = 0;
	private int openScrolls = 0;
	private int openHarrays = 0;

	private int openTrees = 0;		// todo trees
	private int openRows = 0;		// todo trees
	private int openColumns = 0;	// todo trees

	private int openTables = 0;

	private int indentNum = 0;
	private boolean beautify = false;

	private boolean closeDefault = false;


	public BmlForm()
	{
	}

	// Speculation: A block ends with semicolon if it has multiple arguments. Some nulls 
	// generally -- have not checked what these nulls are on any instruction, however.
	public BmlForm(String formTitle)
	{
		addDefaultHeader(formTitle);
	}
	
	public void addDefaultHeader(String formTitle)
	{
		if(closeDefault == true) {
			return;
		}

		beginBorder();
		beginCenter();
		addBoldText(formTitle);
		endCenter();
		
		beginScroll();
		beginVerticalFlow();
		
		closeDefault = true;		// in toString() we close the opened: varray, scroll, border
	}


	public void beginBorder()			{ buf.append(indent("border{")); indentNum++; openBorders++; }
	public void endBorder()				{ indentNum--; buf.append(indent("}")); openBorders--; }

	public void beginCenter()			{ buf.append(indent("center{")); indentNum++; openCenters++; }
	public void endCenter()				{ indentNum--; buf.append(indent("};null;")); openCenters--; }
	
	public void beginVerticalFlow()		{ buf.append(indent("varray{rescale=\"true\";")); indentNum++; openVarrays++; }
	public void endVerticalFlow()		{ indentNum--; buf.append(indent("}")); openVarrays--; }
	
	public void beginScroll()			{ buf.append(indent("scroll{vertical=\"true\";horizontal=\"false\";")); indentNum++; openScrolls++; }
	public void endScroll()				{ indentNum--; buf.append(indent("};null;null;")); openScrolls--; }
	
	public void beginHorizontalFlow()	{ buf.append(indent("harray {")); indentNum++; openHarrays++; }
	public void endHorizontalFlow()		{ indentNum--; buf.append(indent("}")); openHarrays--; }

	public void beginTable(int rowCount, String[] columns)
	{
		buf.append(indent("table {rows=\"" + rowCount + "\"; cols=\"" + columns.length + "\";"));

		indentNum++;
		for(String c : columns) {
			addLabel(c);
		}
		indentNum--;

		indentNum++;
		openTables++;
	}
	public void endTable()				{ indentNum--; buf.append(indent("}")); openTables--; }

	public void addBoldText(String text, String... args)
	{
		addText(text, "bold", args);
	}

	public void addHidden(String name, String val)
	{
		buf.append(indent("passthrough{id=\"" + name + "\";text=\"" + val + "\"}"));
	}
	
	public void addText(String text, String... args)
	{
		addText(text, "", args);
	}
	
	private String indent(String s)
	{
		return (beautify ? getIndentation() + s + "\r\n" : s);
	}
	
	private String getIndentation()
	{
		if(indentNum > 0) {
			return tabs.substring(0, indentNum);
		}
		return "";
	}

	public void addRaw(String s)
	{
		buf.append(s);
	}
	
	public void addImage(String url, int height, int width)
	{
		addImage(url, height, width, "");
	}

	public void addImage(String url, int height, int width, String tooltip)
	{
		buf.append("image{src=\"");
		buf.append(url);				// e.g. "img.gui.bridge.north"
		buf.append("\";size=\"");
		buf.append(height + "," + width);
		buf.append("\";text=\"" + tooltip + "\"}");
	}
	
	public void addLabel(String text)
	{
		buf.append("label{text='" + text + "'};");
	}
	
	public void addInput(String id, int maxChars, String defaultText)
	{
		buf.append("input{id='" + id + "';maxchars='" + maxChars + "';text=\"" + defaultText + "\"};");
	}
	
	private void addText(String text, String type, String... args)
	{
		String[] lines = text.split("\n");
		
		for(String l : lines) {
			if(beautify) {
				buf.append(getIndentation());
			}

			buf.append("text{");
			if(type.equals("") == false) {
				buf.append("type='" + type + "';");
			}
			buf.append("text=\"");
			
			buf.append(String.format(l, (Object[])args));
			buf.append("\"}");

			if(beautify) {
				buf.append("\r\n");
			}
		}
	}
	
	public void addButton(String name, String id)
	{
		buf.append(indent("button{text='  " + name + "  ';id='" + id + "'}"));
	}

	public String toString()
	{
		if(closeDefault) {
			endVerticalFlow();
			endScroll();
			endBorder();
			closeDefault = false;
		}
		
		if(openCenters != 0 || openVarrays != 0 || openScrolls != 0 || openHarrays != 0 || openBorders != 0 || openTrees != 0 || openRows != 0 || openColumns != 0 || openTables != 0) {
			logger.log(Level.SEVERE, "While finalizing BML unclosed (or too many closed) blocks were found (this will likely mean the BML will not work!):"
					+ " center: " + openCenters
					+ " vert-flows: " + openVarrays
					+ " scroll: " + openScrolls
					+ " horiz-flows: " + openHarrays
					+ " border: " + openBorders
					+ " trees: " + openTrees
					+ " rows: " + openRows
					+ " columns: " + openColumns
					+ " tables: " + openTables
			);
		}

		return buf.toString();
	}
}
