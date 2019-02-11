package core;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import core.modAPI.AdditionalBrainIO;
import core.modAPI.Brain;
//import core.modAPI.BrainDrawer;
import core.modAPI.Button;
import core.modAPI.CreatureAction;
import core.modAPI.CreatureAttribute;
import core.modAPI.CreatureEatBehavior;
import core.modAPI.CreaturePeripheral;
//import core.modAPI.CreaturePeripheralDrawer;
import core.modAPI.TileAttribute;
import core.modAPI.TileDrawer;

public final class ModLoader {
	// ui
	
	protected static final Color MOD_LISTING_BACKGROUND_COLOR = new Color(225, 225, 225);
	protected static final Color MOD_LISTING_BACKGROUND_COLOR_DISABLED = new Color(240, 240, 240);
	private static final int MOD_FRAME_WIDTH = 500;
	private static final int MOD_FRAME_HEIGHT = 500;
	
	// non ui
	
	public static final ArrayList<Class<Button>> buttons = new ArrayList<>();
	public static final ArrayList<Class<TileAttribute>> tileAttributes = new ArrayList<>();
	public static final ArrayList<Class<CreatureAttribute>> creatureAttributes = new ArrayList<>();
	public static final ArrayList<Class<CreatureAction>> creatureActions = new ArrayList<>();
	public static final ArrayList<Class<CreaturePeripheral>> creaturePeripherals = new ArrayList<>();

	public static ArrayList<String> brainOutputs = new ArrayList<>();
	//public static ArrayList<String> brainInputs  = new ArrayList<>();
	//public static ArrayList<CreaturePeripheralDrawer> creaturePeripheralDrawers = new ArrayList<>();
	
	public static Class<Brain> brainModel;
	
	public static CreatureEatBehavior creatureEatBehavior;
	public static TileDrawer          tileDrawer;
//	public static BrainDrawer         brainDrawer;
	/**
	 * recursively looks in folder "mods" for any classes that implemnt any API
	 * interfaces and loads them
	 */
	@SuppressWarnings("unchecked")
	public static void init() {
		// default-required outputs
		brainOutputs.add("hue");
		brainOutputs.add("accelerate");
		brainOutputs.add("turn");
		brainOutputs.add("eat");
		brainOutputs.add("fight");
		brainOutputs.add("reproduce");
		
		ArrayList<Path> paths = new ArrayList<>();
		try {
			Files.find(Paths.get("mods/"),
			           Integer.MAX_VALUE,
			           (filePath, fileAttr) ->  fileAttr.isRegularFile())
			        .forEach(paths::add);
			
			Files.find(Paths.get("bin/"),
			           Integer.MAX_VALUE,
			           (filePath, fileAttr) -> fileAttr.isRegularFile() && !filePath.startsWith("bin/core"))
			        .forEach(paths::add);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(paths);
		
		for(int i = 0; i < paths.size(); i++) {
			String fileName = paths.get(i).getFileName().toString();
			String extention = fileName.split("\\.")[1];
			if(!extention.equals("class"))
				paths.remove(i--);
		}
		
		
		// display the mods in a window
		

		JLabel warningLabel = new JLabel();
		warningLabel.setForeground(Color.RED);
			
		JFrame frame = new JFrame("Mod Select");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // allows me to manually close the window
		frame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
            	System.out.println("Warning text: " + warningLabel.getText());
            	
                if(!warningLabel.getText().equals("")) {
                	JOptionPane.showMessageDialog(null, warningLabel.getText());//WARNING_MESSAGE(warningLabel.getText());
                	return;
                }
            	
            	int confirmLoad = JOptionPane.showConfirmDialog(null, "Are these mods okay?");
                if(confirmLoad == 0) {
                	System.out.println("calling finishLoading with " + paths);
                	finishLoading(paths);
                	frame.dispose();
                }
            }
        });
		
		frame.setSize(500,500);
		
		Panel contentPanel = new Panel();
		BoxLayout vert = new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS);
		contentPanel.setLayout(vert);
		
		JScrollPane pane = new JScrollPane(contentPanel);
		frame.add(pane);
		
		HashMap<Path, Panel> pathPanels = new HashMap<>();
		HashMap<Path, Class> pathClasses = new HashMap<>();
		for(Path p : paths) {
			String fileName = p.getFileName().toString();
			String extention = fileName.split("\\.")[1];
			if(!extention.equals("class"))
				continue;
			
			try {
				String className = p.subpath(1, p.getNameCount()).toString().replace('/', '.');
				className = className.substring(0, className.length()-(".class".length()));
				Class<?> c = Class.forName(className);
				pathClasses.put(p, c);
			} catch (Exception e) {}
		}
		

		//Panel warningPanel = new Panel();
		//warningPanel.setLayout(new BoxLayout(warningPanel, BoxLayout.PAGE_AXIS));
		//warningPanel.add(Box.createHorizontalGlue());
		JButton warningButton = new JButton("warnings");
		warningButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(warningLabel.getText().equals("")) {
					JOptionPane.showMessageDialog(null, "No warnings :)");
				} else {
					JOptionPane.showMessageDialog(null, warningLabel.getText());
				}
			}
		});
		//warningPanel.add(warningButton);
		//contentPanel.add(warningPanel);
		contentPanel.add(warningButton);
		
		int index = 0;
		while(index < paths.size()) {
			index = makeListingForMod(paths, index, contentPanel, pathPanels, pathClasses, warningLabel);
		}
		refreshConflicts(paths, pathPanels, pathClasses, warningLabel);
		
		frame.pack();
		frame.setSize(Math.max(frame.getWidth(), MOD_FRAME_WIDTH), frame.getHeight());
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
	}
	
	private static int makeListingForMod(ArrayList<Path> paths, int startIndex, Panel contentPanel, HashMap<Path, Panel> pathPanels, HashMap<Path, Class> pathClasses, JLabel warningLabel) {
		
		int index = startIndex;
		String currentModname = paths.get(startIndex).getName(1).toString();
		String currentModnameReadable = splitCamelCase(currentModname);
		
		ArrayList<Path> thisModsPaths = new ArrayList<>();
		ArrayList<JCheckBox> thisModsCheckBoxes = new ArrayList<>();
		
		/* Make the header for this mod */ {
			Panel panel = new Panel();
			BoxLayout layout = new BoxLayout(panel, BoxLayout.LINE_AXIS);
			panel.setLayout(layout);
			
			JCheckBox box = new JCheckBox();
			box.setSelected(true);
			box.addItemListener(new ItemListener() { 
				ArrayList<Path> thesePaths = thisModsPaths;
				ArrayList<JCheckBox> theseBoxes = thisModsCheckBoxes;
				
	            public void itemStateChanged(ItemEvent e) {  
	            	 if(e.getStateChange() == 1) {
	            		 paths.addAll(thesePaths);
	            	 } else {
	            		 paths.removeAll(thesePaths);
	            		 for(Path q : thesePaths) {
	            			 Panel temp = new Panel();
		            		 pathPanels.get(q).setBackground(MOD_LISTING_BACKGROUND_COLOR_DISABLED);
	            		 }
	            	 }
	            	 
	            	 for(JCheckBox box : theseBoxes) {
	            		 box.setSelected(e.getStateChange() == 1);
	            	 }
	            	 
	            	 refreshConflicts(paths, pathPanels, pathClasses, warningLabel);
	              }
	           });    
			

			JLabel label = new JLabel(currentModnameReadable);
			Font font = label.getFont();
			font = font.deriveFont(
			    Collections.singletonMap(
			        TextAttribute.SIZE, 20));
			label.setFont(font);
			
			panel.add(box, Component.LEFT_ALIGNMENT);
			panel.add(label, Component.LEFT_ALIGNMENT);
			panel.add(new JLabel("    "));
			panel.add(Box.createHorizontalGlue()); // this forces left alignment (because Component.LEFT_ALIGNMENT didn't work)
			contentPanel.add(panel);
		}
		
		// make listings for every file in this mod
		for(; index < paths.size(); index++) {
			if(!paths.get(index).getName(1).toString().equals(currentModname)) {
				return index;
			}
			Path p = paths.get(index);
			
			Panel panel = new Panel();
			BoxLayout layout = new BoxLayout(panel, BoxLayout.LINE_AXIS);
			panel.setLayout(layout);
			
			
			panel.add(new JLabel("    "), Component.LEFT_ALIGNMENT);
			
			JCheckBox box = new JCheckBox();
			box.setSelected(true);
			box.addItemListener(new ItemListener() { 
				Path path = p;
				
	            public void itemStateChanged(ItemEvent e) {  
	            	 if(e.getStateChange() == 1) {
	            		 paths.add(path);
	            		 System.out.println("added " + path.toString());
	            	 } else {
	            		 paths.remove(path);
	            		 System.out.println("removed " + path.toString());
	            		 pathPanels.get(path).setBackground(MOD_LISTING_BACKGROUND_COLOR_DISABLED);
	            	 }
	            	 
	            	 refreshConflicts(paths, pathPanels, pathClasses, warningLabel);
	              }    
	           });    
			
			thisModsCheckBoxes.add(box);
			
			panel.add(box, Component.LEFT_ALIGNMENT);
			panel.add(new JLabel(p.subpath(2, p.getNameCount()).toString().split("\\.")[0]), Component.LEFT_ALIGNMENT);
			
			// TODO: add horizontal filler
			//panel.add(new Box.Filler(new Dimension(1,1), new Dimension(10000,1), new Dimension(1000,1)));
			
			//panel.setPreferredSize(new Dimension(MOD_FRAME_HEIGHT, MOD_FRAME_WIDTH));
			
			Panel ids = new Panel();
			ids.setLayout(new BoxLayout(ids, BoxLayout.LINE_AXIS));
			addInterfaceIdentifiers(ids, pathClasses.get(p));
			ids.add(new JLabel("    "));
			
			//panel.add(Box.createHorizontalGlue());
			
			Panel groupPanel = new Panel();
			groupPanel.setLayout(new BorderLayout());
			groupPanel.add(panel, BorderLayout.WEST);
			groupPanel.add(ids, BorderLayout.EAST);
			contentPanel.add(groupPanel, Component.LEFT_ALIGNMENT);
			//contentPanel.add(panel, Component.LEFT_ALIGNMENT);
			
			pathPanels.put(p, groupPanel);
		}
		
		return index;
	}
	
	private static void addInterfaceIdentifiers(Panel panel, Class c) {
		System.out.println("hi");
		for (Class<?> inter : c.getInterfaces()) {
			JLabel spacer = new JLabel("     ");
			panel.add(spacer);
			
			if(inter.getCanonicalName().equals("core.modAPI.CreatureEatBehavior")) {
				JLabel l = new JLabel("(Eat Behavior)");
				l.setForeground(Color.ORANGE.darker());
				panel.add(l);
			}
			if(inter.getCanonicalName().equals("core.modAPI.TileDrawer")) {
				// yellow
				JLabel l = new JLabel("(Tile Drawer)");
				l.setForeground(Color.YELLOW.darker());
				panel.add(l);
			}
			if(inter.getCanonicalName().equals("core.modAPI.AdditionalBrainIO")) {
				JLabel l = new JLabel("(+BrainIO)");
				l.setForeground(Color.GREEN);
				panel.add(l);
			}
			
			if(inter.getCanonicalName().equals("core.modAPI.Brain")) {
				// red
				JLabel l = new JLabel("(Brain Model)");
				l.setForeground(Color.RED);
				panel.add(l);
			}
			
			//creature action, brain outputs

			if(inter.getCanonicalName().equals("core.modAPI.Button")) {
				JLabel l = new JLabel("(Button)");
				l.setForeground(new Color(150, 150, 200));
				panel.add(l);
			}
			if(inter.getCanonicalName().equals("core.modAPI.TileAttribute")) {
				JLabel l = new JLabel("(Tile Attribute)");
				l.setForeground(new Color(0, 200, 150));
				panel.add(l);
			}
			if(inter.getCanonicalName().equals("core.modAPI.CreatureAttribute")) {
				JLabel l = new JLabel("(Creature Attribute)");
				l.setForeground(new Color(0, 150, 200));
				panel.add(l);
			}
			if(inter.getCanonicalName().equals("core.modAPI.CreatureAction")) {
				JLabel l = new JLabel("(Creature Action)");
				l.setForeground(new Color(150, 0, 200));
				panel.add(l);
			}
			if(inter.getCanonicalName().equals("core.modAPI.CreaturePeripheral")) {
				JLabel l = new JLabel("(Creature Peripheral)");
				l.setForeground(new Color(200, 0, 150));
				panel.add(l);
			}
		}
	}

	static void refreshConflicts(ArrayList<Path> paths, HashMap<Path, Panel> pathPanels, HashMap<Path, Class> pathClasses, JLabel warningLabel) {
		int brainModelConflict = 0;
		int eatBehaviorConflict = 0;
		int tileDrawerConflict = 0;
		
		for(Path p : paths) {
			pathPanels.get(p).setBackground(MOD_LISTING_BACKGROUND_COLOR);
			
			try {
				Class<?> c = pathClasses.get(p);
				
				for (Class<?> inter : c.getInterfaces()) {
					if(inter.getCanonicalName().equals("core.modAPI.CreatureEatBehavior")) {
						eatBehaviorConflict++; 
					}
					if(inter.getCanonicalName().equals("core.modAPI.TileDrawer")) {
						tileDrawerConflict++;
					}
					
					if(inter.getCanonicalName().equals("core.modAPI.Brain")) {
						brainModelConflict++;
					}
				}
			} catch (Exception e) {}
		}
		
		String warning = "";
		
		if(eatBehaviorConflict == 0) {
			warning += " No CreatureEatBehavior implementation selected!";
		}
		if(brainModelConflict == 0) {
			warning += " No Brain implementation selected!";
		}
		if(tileDrawerConflict == 0) {
			warning += " No TileDrawer implementation selected!";
		}
		if(eatBehaviorConflict > 1) {
			warning += " Too many CreatureEatBehavior implementations selected!";
		}
		if(brainModelConflict > 1) {
			warning += " Too many Brain implementations selected!";
		}
		if(tileDrawerConflict > 1) {
			warning += " Too many TileDrawer implementation selected!";
		}
		warningLabel.setText(warning);
		
		if(eatBehaviorConflict == 1 && brainModelConflict == 1 && tileDrawerConflict == 1) {
			return;
		}
		
		for(Path p : paths) {
			String fileName = p.getFileName().toString();
			String extention = fileName.split("\\.")[1];
			if(!extention.equals("class"))
				continue;
			
			try {
				Class<?> c = pathClasses.get(p);
				
				for (Class<?> inter : c.getInterfaces()) {
					if((eatBehaviorConflict > 1 && inter.getCanonicalName().equals("core.modAPI.CreatureEatBehavior"))) {
						Panel pan = pathPanels.get(p);
						pan.setBackground(Color.ORANGE);
					}
					if(tileDrawerConflict > 1 && inter.getCanonicalName().equals("core.modAPI.TileDrawer")) {
						Panel pan = pathPanels.get(p);
						pan.setBackground(Color.YELLOW);
					}
					if(brainModelConflict > 1 && inter.getCanonicalName().equals("core.modAPI.Brain")) {
						Panel pan = pathPanels.get(p);
						pan.setBackground(Color.RED);
					}
				}
			} catch (Exception e) {}
		}
	} 
	
	// function credit to polygenelubricants on StackOverflow
	// https://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
	private static String splitCamelCase(String string) {
		if(string.equals("")) return "";
		
		string = Character.toUpperCase(string.charAt(0)) + string.substring(1);
		
		return string.replaceAll(
			      String.format("%s|%s|%s",
			         "(?<=[A-Z])(?=[A-Z][a-z])",
			         "(?<=[^A-Z])(?=[A-Z])",
			         "(?<=[A-Za-z])(?=[^A-Za-z])"
			      ),
			      " "
			   );
	}

	private static void finishLoading(ArrayList<Path> modList) {
		// load the mods
		
		System.out.println("final modlist " + modList);
		
		for (Path p : modList) {
			// if the path doesn't end with ".class", skip this one
			String fileName = p.getFileName().toString();
			String extention = fileName.split("\\.")[1];
			if(!extention.equals("class"))
				continue;
			
			try {
				String className = p.subpath(1, p.getNameCount()).toString().replace('/', '.');
				className = className.substring(0, className.length()-(".class".length()));
				Class<?> c = Class.forName(className);
				
				System.out.println(c);
				
				// WHEN YOU ADD MORE MOD API INTERFACES, ADD AN IF STATEMENT FOR EACH OF
				// THEM HERE, THAT WILL TAKE CARE OF LOADING THEM
				
				// TO ADD MODS, SIMPLY DROP A FOLDER OF THE .class FILES IN EITHER THE 
				// "bin" FOLDER, OR THE "mods" FOLDER
				for (Class<?> inter : c.getInterfaces()) {
					System.out.println("\t"+inter.getCanonicalName());
					
					if(inter.getCanonicalName().equals("core.modAPI.CreatureEatBehavior")) {
						creatureEatBehavior = (CreatureEatBehavior) c.getConstructor().newInstance(); 
					}
//							if(inter.getCanonicalName().equals("core.modAPI.CreaturePeripheralDrawer")) {
//								creaturePeripheralDrawers.add((CreaturePeripheralDrawer) c.getConstructor().newInstance()); 
//							}
					if(inter.getCanonicalName().equals("core.modAPI.TileDrawer")) {
						tileDrawer = (TileDrawer) c.getConstructor().newInstance(); 
					}
//					if(inter.getCanonicalName().equals("core.modAPI.BrainDrawer")) {
//						brainDrawer = (BrainDrawer) c.getConstructor().newInstance(); 
//					}
					if(inter.getCanonicalName().equals("core.modAPI.AdditionalBrainIO")) {
						AdditionalBrainIO io = ((AdditionalBrainIO) c.getConstructor().newInstance());
						brainOutputs.addAll(io.getOutputs());
						//brainInputs. addAll(io.getInputs());
					}
					
					if(inter.getCanonicalName().equals("core.modAPI.Brain")) {
						brainModel = (Class<Brain>) c;
					}
					
					//creature action, brain outputs

					if(inter.getCanonicalName().equals("core.modAPI.Button")) {
						buttons.add((Class<Button>) c);
					}
					if(inter.getCanonicalName().equals("core.modAPI.TileAttribute")) {
						tileAttributes.add((Class<TileAttribute>) c);
					}
					if(inter.getCanonicalName().equals("core.modAPI.CreatureAttribute")) {
						creatureAttributes.add((Class<CreatureAttribute>) c);
					}
					if(inter.getCanonicalName().equals("core.modAPI.CreatureAction")) {
						creatureActions.add((Class<CreatureAction>) c);
					}
					if(inter.getCanonicalName().equals("core.modAPI.CreaturePeripheral")) {
						creaturePeripherals.add((Class<CreaturePeripheral>) c);
					}
//							if(inter.getCanonicalName().equals("core.modAPI.BrainInput")) {
//								brainInputs.add((Class<BrainInput>) c);
//							}
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println(tileAttributes);
		String missing = "";
		missing += brainModel          == null? "Brain.java"               : "";  
//		missing += brainDrawer         == null? "BrainDrawer.java"         : ""; 
		missing += creatureEatBehavior == null? "CreatureEatBehavior.java" : ""; 
		missing += tileDrawer          == null? "TileDrawer.java"          : ""; 
		
		if(!missing.equals("")) {
			JOptionPane.showConfirmDialog(null, "Warning: missing implementations for some critical interfaces. Please try enabling more mods.\nMissing implementations:\n"+missing);
		}
		
		EvolvioMod.main.finishSetup();
		
//		EvolvioMod.finishStartup(); // temp
	}

	public static void initializeAttributes(Tile tile, Board board, float stepSize) {
		for(Class<TileAttribute> attribute : tileAttributes) {
			try {
				TileAttribute a = attribute.newInstance();
				a.init(tile.posX, tile.posY, stepSize, board, tile);
				tile.attributes.put(a.getName(), a);
			} catch (InstantiationException | IllegalAccessException e) { e.printStackTrace(); }
		}
	}
	
	public static void initializeAttributes(Creature creature, Board board) {
		for(Class<CreatureAttribute> attribute : creatureAttributes) {
			try {
				CreatureAttribute a = attribute.newInstance();
				a.init(board, creature);
				creature.attributes.put(a.getName(), a);
			} catch (InstantiationException | IllegalAccessException e) { e.printStackTrace(); }
		}
		for(Class<CreatureAction> action : creatureActions) {
			try {
				CreatureAction a = action.newInstance();
				creature.actions.add(a);
			} catch (InstantiationException | IllegalAccessException e) { e.printStackTrace(); }
		}
		for(Class<CreaturePeripheral> peripheral : creaturePeripherals) {
			try {
				CreaturePeripheral p = peripheral.newInstance();
				creature.peripherals.add(p);
			} catch (InstantiationException | IllegalAccessException e) { e.printStackTrace(); }
		}
	}
	
	public static List<CreaturePeripheral> createPeripherals() {
		List<CreaturePeripheral> p = new ArrayList<>();
		for(Class<CreaturePeripheral> cl : creaturePeripherals) {
			try {
				CreaturePeripheral per = (cl.getConstructor().newInstance());
				per.init();
				p.add(per);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		
		return p;
	}

	public static Brain createBrain(Creature c, Board b) {
		// create peripherals
		ArrayList<String> inputs = new ArrayList<>();
		for(CreaturePeripheral peripheral : c.peripherals) {
			inputs.addAll(peripheral.getInputNames());
		}
		
		try {
//			ArrayList<BrainInput> inputs = new ArrayList<>();
//			for(Class<BrainInput> in : brainInputs) {
//				BrainInput inp = in.getConstructor().newInstance();
//				inputs.add(inp);
//			}
			//ArrayList<String> inputs = new ArrayList<>();
			//inputs.addAll(brainInputs);

			ArrayList<String> outputs = new ArrayList<>();
			outputs.addAll(brainOutputs);
			
			Brain brain = brainModel.getConstructor().newInstance();
			brain.init(c, b, inputs, outputs);
			
			return brain;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
	}

	public static Brain getOffspringBrain(List<CreaturePeripheral> babyPeripherals, ArrayList<Creature> parents) {
		ArrayList<String> inputs = new ArrayList<>();
		for(CreaturePeripheral peripheral : babyPeripherals) {
			inputs.addAll(peripheral.getInputNames());
		}
		
		try {
//			ArrayList<BrainInput> inputs = new ArrayList<>();
//			for(Class<BrainInput> in : brainInputs) {
//				BrainInput inp = in.getConstructor().newInstance();
//				inputs.add(inp);
//			}
//			ArrayList<String> inputs = new ArrayList<>();
//			inputs.addAll(brainInputs);
//			
			ArrayList<String> outputs = new ArrayList<>();
			outputs.addAll(brainOutputs);
			
			return brainModel.getConstructor().newInstance().getOffspring(parents, inputs, outputs);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		return null;
	}

	public static void setOffspringAttributes(Creature baby, ArrayList<Creature> parents, Board board) {
		System.out.println(parents);
		for(Class<CreatureAttribute> attribute : creatureAttributes) {
			try {
				String attributeName = attribute.getConstructor().newInstance().getName();
				System.out.println(attributeName);
				
				ArrayList<CreatureAttribute> parentAttributes = new ArrayList<>();
				for(Creature parent : parents) {
					parentAttributes.add(parent.getAttribute(attributeName));
				}
				
				CreatureAttribute a = attribute.newInstance();
				a.initFromParents(parentAttributes, board);
				
				baby.attributes.put(a.getName(), a);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) { e.printStackTrace(); }
		}
	}
}
