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
package org.jdrupes.mdoclet.processors;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdrupes.mdoclet.MarkdownProcessor;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;

/**
 * This class provides an adapter for the 
 * [flexmark-java](https://github.com/vsch/flexmark-java) Markdown
 * processor.
 * 
 * The adapter supports the following flags:
 * 
 * `-parser-profile` 
 * :   Sets one of the profiles defined in
 *     `com.vladsch.flexmark.parser.ParserEmulationProfile`. The name of the
 *     profle is the lower case name of the enum value. At the time of this
 *     writing supported names are:
 *      - commonmark (default)
 *      - fixed_indent
 *      - kramdown
 *      - markdown
 *      - github_doc
 *      - multi_markdown
 *      - pegdown
 * 
 * `-clear-extensions`
 * :   Clears the list of extensions. The following extensions are predefined:
 *       - [Abbreviation](https://github.com/vsch/flexmark-java/wiki/Extensions#abbreviation)
 *       - [Definition](https://github.com/vsch/flexmark-java/wiki/Extensions#definition-lists) 
 *         (Definition Lists)[^DefLists]
 *       - [Footnote](https://github.com/vsch/flexmark-java/wiki/Extensions#footnotes)
 *       - [Tables](https://github.com/vsch/flexmark-java/wiki/Extensions#tables)
 * 
 * `-extension <name>`
 * :   Adds the flexmark extension with the given name to the list of extensions.
 *     If the name contains a dot, it is assumed to be a fully qualified class
 *     name. Else, it is expanded using the naming pattern used by flexmark.
 *     
 * The parser also supports disabling the automatic highlight feature.
 * 
 * [^DefLists]: If you use this extension, you'll most likely want to supply a
 *     modified style sheet because the standard stylesheet assumes all definition
 *     lists to be parameter defintion lists and formats them accordingly.
 *     
 *     Here are the changes made for this documentation:
 *     ```css
 *     /* [MOD] {@literal *}/
 *     /* .contentContainer .description dl dd, {@literal *}/ .contentContainer .details dl dt, .serializedFormContainer dl dt {
 *         font-size:12px;
 *         font-weight:bold;
 *         margin:10px 0 0 0;
 *         color:#4E4E4E;
 *     }
 *     /* [MOD] Added {@literal *}/
 *     dl dt {
 *         margin:10px 0 0 0;
 *     }
 *     
 *     /* [MOD] {@literal *}/
 *     /* .contentContainer .description dl dd, {@literal *}/ .contentContainer .details dl dd, .serializedFormContainer dl dd {
 *         margin:5px 0 10px 0px;
 *         font-size:14px;
 *         font-family:'DejaVu Sans Mono',monospace;
 *     }
 *     ```
 * 
 */
public class FlexmarkProcessor implements MarkdownProcessor {

	final private static String OPT_PROFILE = "-parser-profile";
	final private static String OPT_CLEAR_EXTENSIONS = "-clear-extensions";
	final private static String OPT_EXTENSION = "-extension";
	
	private Parser parser;
	private HtmlRenderer renderer;
	
	@Override
	public int isSupportedOption(String option) {
		switch (option) {
		case OPT_CLEAR_EXTENSIONS:
		case INTERNAL_OPT_DISABLE_AUTO_HIGHLIGHT:
			return 0;
			
		case OPT_PROFILE:
		case OPT_EXTENSION:
			return 1;
		default:
			return -1;
		}
	}


	@Override
	public void start(String[][] options) {
        MutableDataSet flexmarkOpts = new MutableDataSet();
        Set<Class<? extends Extension>> extensions = new HashSet<>();
        extensions.add(AbbreviationExtension.class);
        extensions.add(DefinitionExtension.class);
        extensions.add(FootnoteExtension.class);
        extensions.add(TablesExtension.class);
        extensions.add(TypographicExtension.class);
        extensions.add(WikiLinkExtension.class);

        for (String[] opt: options) {
        	switch (opt[0]) {
        	case OPT_PROFILE:
        		setFromProfile(flexmarkOpts, opt[1]);
        		continue;
        		
        	case OPT_CLEAR_EXTENSIONS:
        		extensions.clear();
        		continue;
        		
        	case OPT_EXTENSION:
        		try {
        			String clsName = opt[1];
        			if (!clsName.contains(".")) {
        				clsName = "com.vladsch.flexmark.ext."
        					+ opt[1].toLowerCase() + "." + opt[1] + "Extension";
        			}
        			@SuppressWarnings("unchecked") 
        			Class<? extends Extension> cls = (Class<? extends Extension>) 
        				getClass().getClassLoader().loadClass(clsName);
        			extensions.add(cls);
        			continue;
        		} catch (ClassNotFoundException | ClassCastException e) {
        			throw new IllegalArgumentException
        				("Cannot find extension " + opt[1]
        					+ " (check spelling and classpath).");
        		}
        	
        	case INTERNAL_OPT_DISABLE_AUTO_HIGHLIGHT:
        		flexmarkOpts.set
        			(HtmlRenderer.FENCED_CODE_NO_LANGUAGE_CLASS, "nohighlight");
        		continue;

        	default:
            	throw new IllegalArgumentException("Unknown option: " + opt[0]);
        	}
        }
        
        List<Extension> extObjs = new ArrayList<>();
        for (Class<? extends Extension> cls: extensions) {
        	try {
				extObjs.add((Extension)cls.getMethod("create").invoke(null));
			} catch (IllegalAccessException | IllegalArgumentException 
					| InvocationTargetException | NoSuchMethodException 
					| SecurityException e) {
				throw new IllegalArgumentException("Cannot create extension of type "
						+ cls + ".");
			}
        }
        if (!extObjs.isEmpty()) {
        	flexmarkOpts.set(Parser.EXTENSIONS, extObjs);
        }
        parser = Parser.builder(flexmarkOpts).build();
        renderer = HtmlRenderer.builder(flexmarkOpts).build();
	}

	private void setFromProfile(MutableDataSet fmOpts, String profileName) {
		for (ParserEmulationProfile p: ParserEmulationProfile.values()) {
			if (p.toString().equalsIgnoreCase(profileName)) {
				fmOpts.setFrom(p);
				return;
			}
		}
		throw new IllegalArgumentException("Unknown profile: " + profileName);
	}


	/* (non-Javadoc)
	 * @see org.jdrupes.mdoclet.MarkdownProcessor#toHtml(java.lang.String)
	 */
	@Override
	public String toHtml(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
	}

}
