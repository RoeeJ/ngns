/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.anticheat;

import java.io.Serializable;

/**
 *
 * @author Matze
 */
public class CheaterData implements Serializable, Comparable<CheaterData> {
	//private static final long serialVersionUID = -8733673311051249885L;
	
	private int points;
	private String info;

	public CheaterData(int points, String info) {
		this.points = points;
		this.info = info;
	}

	public String getInfo() {
		return info;
	}

	public int getPoints() {
		return points;
	}

	public int compareTo(CheaterData o) {
		int thisVal = getPoints();
		int anotherVal = o.getPoints();
		return (thisVal<anotherVal ? 1 : (thisVal==anotherVal ? 0 : -1));
	}
}
