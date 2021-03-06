package evolvioOriginal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Board;
import core.Creature;
import core.EvolvioMod;
import core.SoftBody;
import core.modAPI.CreatureFeatureDrawer;
import core.modAPI.CreaturePeripheral;
import processing.core.PConstants;

public class Eyestalks implements CreaturePeripheral, CreatureFeatureDrawer {

	float CROSS_SIZE = 0.022f;
	final float BRIGHTNESS_THRESHOLD = 0.7f;
	
	double[] visionAngles = { 0, -0.4, 0.4 };
	double[] visionDistances = { 0, 0.7, 0.7 };
	double maxVisionDistance;
	// double visionAngle;
	// double visionDistance;
	private double[] visionOccludedX = new double[visionAngles.length];
	private double[] visionOccludedY = new double[visionAngles.length];
	double visionResults[] = new double[9];

	//p List<String> inputNames;
	
	private void setVisionOcclusion(int i, double x, double y) {
		if(i != 0 && (x == 0 || y == 0)) {
			System.err.println("Eyestalk whoops!");
			
			for(StackTraceElement e : Thread.currentThread().getStackTrace()) {
				System.err.println("\t" + e);
			}
		}
		
		visionOccludedX[i] = x;
		visionOccludedY[i] = y;
	}
	
	@Override
	public void init() {
		maxVisionDistance = visionDistances[0];
		for(double d : visionDistances) {
			maxVisionDistance = Math.max(maxVisionDistance, d);
		}
	}

	@Override
	public List<String> getInputNames() {
		List<String> list = new ArrayList<>();

		list.add("0Hue");
		list.add("0Sat");
		list.add("0Bri");
		list.add("1Hue");
		list.add("1Sat");
		list.add("1Bri");
		list.add("2Hue");
		list.add("2Sat");
		list.add("2Bri");
		
		return list;
	}
	
	@Override
	public Map<String, Double> getInputValues(Creature creature, Board board, double timeStep) {
		for (int k = 0; k < visionAngles.length; k++) {
			double visionStartX = creature.px;
			double visionStartY = creature.py;
			double visionTotalAngle = creature.rotation + visionAngles[k];

			double endX = getVisionEndX(k, creature);
			double endY = getVisionEndY(k, creature);
			//System.out.printf("creature: (%f, %f) vision: (%f, %f) \n", creature.px, creature.py, endX, endY);
			
			setVisionOcclusion(k, endX, endY);
			int c = creature.getColorAt(endX, endY);
			visionResults[k * 3] = EvolvioMod.main.hue(c);
			visionResults[k * 3 + 1] = EvolvioMod.main.saturation(c);
			visionResults[k * 3 + 2] = EvolvioMod.main.brightness(c);
			
			List<SoftBody> pvo = board.getSoftBodiesInArea(visionStartX-maxVisionDistance, visionStartY-maxVisionDistance, maxVisionDistance*2f, maxVisionDistance*2f);
			
			double[][] rotationMatrix = new double[2][2];
			rotationMatrix[1][1] = rotationMatrix[0][0] = Math.cos(-visionTotalAngle);
			rotationMatrix[0][1] = Math.sin(-visionTotalAngle);
			rotationMatrix[1][0] = -rotationMatrix[0][1];
			double visionLineLength = visionDistances[k];
			for (int i = 0; i < pvo.size(); i++) {
				SoftBody body = pvo.get(i);
				double x = body.px - creature.px;
				double y = body.py - creature.py;
				double r = body.getRadius();
				double translatedX = rotationMatrix[0][0] * x + rotationMatrix[1][0] * y;
				double translatedY = rotationMatrix[0][1] * x + rotationMatrix[1][1] * y;
				if (Math.abs(translatedY) <= r) {
					if ((translatedX >= 0 && translatedX < visionLineLength && translatedY < visionLineLength)
							|| distance(0, 0, translatedX, translatedY) < r
							|| distance(visionLineLength, 0, translatedX, translatedY) < r) { // YES! There is an
																								// occlussion.
						visionLineLength = translatedX - Math.sqrt(r * r - translatedY * translatedY);
						
						double visX = visionStartX + visionLineLength * Math.cos(visionTotalAngle);
						double visY = visionStartY + visionLineLength * Math.sin(visionTotalAngle);
						setVisionOcclusion(k, visX, visY);
						
						visionResults[k * 3] = body.hue;
						visionResults[k * 3 + 1] = body.saturation;
						visionResults[k * 3 + 2] = body.brightness;
					}
				}
			}
		}
		
		HashMap<String, Double> vals = new HashMap<>();
		List<String> inputNames = getInputNames();
		for(int i = 0; i < inputNames.size(); i++) {
			vals.put(inputNames.get(i), visionResults[i]);
		}
		
		return vals;
	}
	
//	public void addPVOs(int x, int y, Creature creature, Board board, ArrayList<SoftBody> PVOs) {
//		if (x >= 0 && x < board.boardWidth && y >= 0 && y < board.boardHeight) {
//			for (int i = 0; i < board.softBodiesInPositions[x][y].size(); i++) {
//				SoftBody newCollider = (SoftBody) board.softBodiesInPositions[x][y].get(i);
//				if (!PVOs.contains(newCollider) && newCollider != creature) {
//					PVOs.add(newCollider);
//				}
//			}
//		}
//	}

	public double distance(double x1, double y1, double x2, double y2) {
		return (Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
	}
	
	public double getVisionEndX(int i, Creature c) {
		double visionTotalAngle = c.rotation + visionAngles[i];
		return c.px + visionDistances[i] * Math.cos(visionTotalAngle);
	}

	public double getVisionEndY(int i, Creature c) {
		double visionTotalAngle = c.rotation + visionAngles[i];
		return c.py + visionDistances[i] * Math.sin(visionTotalAngle);
	}
	
	@Override
	public void preCreatureDraw(Creature creature, Board board, float scaleUp, boolean overworldDraw) {
		if(!overworldDraw) {
			return;
		}
		
		// TODO: eyeballs don't draw properly
		
		EvolvioMod.main.ellipseMode(PConstants.RADIUS);
		
		for (int i = 0; i < visionAngles.length; i++) {
			int visionUIcolor = EvolvioMod.main.color(0, 0, 1);
			if (visionResults[i * 3 + 2] > BRIGHTNESS_THRESHOLD) {
				visionUIcolor = EvolvioMod.main.color(0, 0, 0);
			}
			EvolvioMod.main.stroke(visionUIcolor);
			EvolvioMod.main.strokeWeight(board.CREATURE_STROKE_WEIGHT);
			float endX = (float) getVisionEndX(creature, i);
			float endY = (float) getVisionEndY(creature, i);
			
			EvolvioMod.main.line((float) (creature.px * scaleUp), (float) (creature.py * scaleUp), endX * scaleUp, endY * scaleUp);
			EvolvioMod.main.noStroke();
			EvolvioMod.main.fill(visionUIcolor);
			EvolvioMod.main.ellipse((float) (visionOccludedX[i] * scaleUp), (float) (visionOccludedY[i] * scaleUp),
					2 * CROSS_SIZE * scaleUp, 2 * CROSS_SIZE * scaleUp);
			
			//System.out.printf("eyeball: (%f, %f)  vision end: (%f, %f)\n", (float) (visionOccludedX[i] * scaleUp), (float) (visionOccludedY[i] * scaleUp), endX, endY);
			
			EvolvioMod.main.stroke((float) (visionResults[i * 3]), (float) (visionResults[i * 3 + 1]),
					(float) (visionResults[i * 3 + 2]));
			EvolvioMod.main.strokeWeight(board.CREATURE_STROKE_WEIGHT);
			EvolvioMod.main.line((float) ((visionOccludedX[i] - CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedY[i] - CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedX[i] + CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedY[i] + CROSS_SIZE) * scaleUp));
			EvolvioMod.main.line((float) ((visionOccludedX[i] - CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedY[i] + CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedX[i] + CROSS_SIZE) * scaleUp),
					(float) ((visionOccludedY[i] - CROSS_SIZE) * scaleUp));
			
			//System.out.println(i + " " + visionOccludedX[i] + ", " + visionOccludedY[i]);
		}
	}
	
	public double getVisionEndX(Creature creature, int i) {
		double visionTotalAngle = creature.rotation + visionAngles[i];
		return creature.px + visionDistances[i] * Math.cos(visionTotalAngle);
	}

	public double getVisionEndY(Creature creature, int i) {
		double visionTotalAngle = creature.rotation + visionAngles[i];
		return creature.py + visionDistances[i] * Math.sin(visionTotalAngle);
	}

	// draws the middle / ground eye
	@Override
	public void postCreatureDraw(Creature creature, Board board, float scaleUp, boolean overworldDraw) {
//		if(!overworldDraw) {
//			return;
//		}
//		
//		EvolvioMod.main.ellipseMode(PConstants.RADIUS);
//		int i = 0;
//		int visionUIcolor = EvolvioMod.main.color(0, 0, 1);
//		if (visionResults[i * 3 + 2] > BRIGHTNESS_THRESHOLD) {
//			visionUIcolor = EvolvioMod.main.color(0, 0, 0);
//		}
//		EvolvioMod.main.stroke(visionUIcolor);
//		EvolvioMod.main.strokeWeight(board.CREATURE_STROKE_WEIGHT);
//		EvolvioMod.main.noStroke();
//		EvolvioMod.main.fill(visionUIcolor);
//		EvolvioMod.main.ellipse((float) (visionOccludedX[i] * scaleUp), (float) (visionOccludedY[i] * scaleUp),
//				2 * CROSS_SIZE * scaleUp, 2 * CROSS_SIZE * scaleUp);
//		EvolvioMod.main.stroke((float) (visionResults[i * 3]), (float) (visionResults[i * 3 + 1]),
//				(float) (visionResults[i * 3 + 2]));
//		EvolvioMod.main.strokeWeight(board.CREATURE_STROKE_WEIGHT);
//		EvolvioMod.main.line((float) ((visionOccludedX[i] - CROSS_SIZE) * scaleUp),
//				(float) ((visionOccludedY[i] - CROSS_SIZE) * scaleUp),
//				(float) ((visionOccludedX[i] + CROSS_SIZE) * scaleUp),
//				(float) ((visionOccludedY[i] + CROSS_SIZE) * scaleUp));
//		EvolvioMod.main.line((float) ((visionOccludedX[i] - CROSS_SIZE) * scaleUp),
//				(float) ((visionOccludedY[i] + CROSS_SIZE) * scaleUp),
//				(float) ((visionOccludedX[i] + CROSS_SIZE) * scaleUp),
//				(float) ((visionOccludedY[i] - CROSS_SIZE) * scaleUp));
	}
}
