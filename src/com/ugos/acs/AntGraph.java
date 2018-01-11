/**
 * AntGraph.java
 *
 * @author Created by Omnicore CodeGuide
 */
package com.ugos.acs;

import java.io.Serializable;
import java.util.Vector;

public abstract class AntGraph implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected double[][] m_delta;
	private double[][] m_tau;
	private int m_nNodes;
	private double m_dTau0;

	public AntGraph(int nNodes, double[][] delta, double[][] tau) {
		if (delta.length != nNodes)
			throw new IllegalArgumentException("The number of nodes doesn't match with the dimension of delta matrix");

		m_nNodes = nNodes;
		m_delta = delta; // Abstand
		m_tau = tau; // Phaeromon
	}

	public AntGraph(int nodes, double[][] delta) {
		this(nodes, delta, new double[nodes][nodes]);

		resetTau();
	}

	public synchronized double delta(int r, int s) {
		return m_delta[r][s];
	}

	public abstract double delta(int r, int s, Vector path);

	public synchronized double tau(int r, int s) {
		return m_tau[r][s];
	}

	public synchronized double etha(int r, int s) {
		return ((double) 1) / delta(r, s);
	}

	// public synchronized double etha(int r, int s)
	// {
	// return ((double)1) / delta(r, s);
	// }

	public synchronized int nodes() {
		return m_nNodes;
	}

	public synchronized double tau0() {
		return m_dTau0;
	}

	public synchronized void updateTau(int r, int s, double value) {
		m_tau[r][s] = value;
	}

	public void resetTau() {
		double dAverage = averageDelta();

		m_dTau0 = (double) 1 / ((double) m_nNodes * (0.5 * dAverage));

		System.out.println("Average: " + dAverage);
		System.out.println("Tau0: " + m_dTau0);

		for (int r = 0; r < nodes(); r++) {
			for (int s = 0; s < nodes(); s++) {
				m_tau[r][s] = m_dTau0;
			}
		}
	}

	public double averageDelta() {
		return average(m_delta);
	}

	public double averageTau() {
		return average(m_tau);
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		StringBuilder str1 = new StringBuilder();

		for (int r = 0; r < nodes(); r++) {
			for (int s = 0; s < nodes(); s++) {
				// str += delta(r,s) + "\t";
				str1.append(tau(r, s)).append("\t");
			}

			str.append("\n");
		}

		return str + "\n\n\n" + str1;
	}

	private double average(double matrix[][]) {
		double dSum = 0;
		for (int r = 0; r < m_nNodes; r++) {
			for (int s = 0; s < m_nNodes; s++) {
				dSum += matrix[r][s];
			}
		}

		return dSum / (double) (m_nNodes * m_nNodes);
	}
}
