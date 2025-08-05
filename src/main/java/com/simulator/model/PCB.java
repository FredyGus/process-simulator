package com.simulator.model;

public class PCB {
    private static int nextPid = 1;
    private final int pid;
    private final String name;
    private final int arrivalTime;
    private final int burstTime;
    private int remainingTime;
    private int priority;
    private State state;

    public enum State { NEW, READY, RUNNING, WAITING, TERMINATED }

    public PCB(String name, int arrivalTime, int burstTime, int priority) {
        this.pid = nextPid++;
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;
        this.state = State.NEW;
    }

        // Getters
    public int getPid() { return pid; }
    public String getName() { return name; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getRemainingTime() { return remainingTime; }
    public int getPriority() { return priority; }
    public State getState() { return state; }

    // Setters
    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setState(State state) {
        this.state = state;
    }
}