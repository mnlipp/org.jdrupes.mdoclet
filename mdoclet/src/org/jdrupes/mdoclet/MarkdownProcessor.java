/*
 * JDrupes MDoclet
 * Copyright (C) 2017  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.jdrupes.mdoclet;

import javax.tools.OptionChecker;

/**
 * Provides the interface to the Markdown processor.
 */
public interface MarkdownProcessor extends OptionChecker {

	final String INTERNAL_OPT_DISABLE_AUTO_HIGHLIGHT
		= "_disable-auto-highlight_";
	
	/**
	 * Starts the processor with the given options.
	 * 
	 * All processors should support the special option `_disable-auto-highlight_`.
	 * The doclet maps its option `-disable-auto-highlight` to this special
	 * processor option because disabling the auto highlight feature is usually
	 * implemented by configuring the processors HTML renderer in some way.
	 *  
	 * @param options an array of options; each entry consists of an array that has
	 * the option name as first entry and any parameters for that option as subsequent
	 * entries
	 */
	void start (String[][] options);
	
	/**
	 * Converts the given markdown test to HTML.
	 * 
	 * @param markdown the markdoen text
	 * @return the HTML
	 */
	String toHtml(String markdown);

}
