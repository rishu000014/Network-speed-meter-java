package com.netspeedmeter.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class SpeedGraph extends Pane {

    private static final int MAX_POINTS = 90;
    private static final Color DOWNLOAD = Color.web("#3DDCFF");
    private static final Color UPLOAD = Color.web("#FF7A45");

    private final Canvas canvas = new Canvas();
    private final Deque<Double> downloads = new ArrayDeque<>();
    private final Deque<Double> uploads = new ArrayDeque<>();

    public SpeedGraph() {
        getStyleClass().add("graph-canvas-wrap");
        getChildren().add(canvas);
        widthProperty().addListener((obs, oldV, newV) -> redraw());
        heightProperty().addListener((obs, oldV, newV) -> redraw());
    }

    public void addSample(double downloadMbps, double uploadMbps) {
        downloads.addLast(downloadMbps);
        uploads.addLast(uploadMbps);
        while (downloads.size() > MAX_POINTS) {
            downloads.removeFirst();
        }
        while (uploads.size() > MAX_POINTS) {
            uploads.removeFirst();
        }
        redraw();
    }

    public void clear() {
        downloads.clear();
        uploads.clear();
        redraw();
    }

    @Override
    protected void layoutChildren() {
        canvas.setLayoutX(0);
        canvas.setLayoutY(0);
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#08101C"));
        gc.fillRoundRect(0, 0, width, height, 18, 18);

        double left = 58;
        double right = 18;
        double top = 22;
        double bottom = 28;
        double plotW = Math.max(1, width - left - right);
        double plotH = Math.max(1, height - top - bottom);

        double max = 1.0;
        for (double value : downloads) {
            max = Math.max(max, value);
        }
        for (double value : uploads) {
            max = Math.max(max, value);
        }
        max = niceMax(max);

        gc.setStroke(Color.web("#1C2A40"));
        gc.setLineWidth(1);
        gc.setFont(Font.font("Segoe UI", 11));
        gc.setFill(Color.web("#7F8DA3"));
        int lines = 4;
        for (int i = 0; i <= lines; i++) {
            double y = top + plotH * i / lines;
            gc.strokeLine(left, y, left + plotW, y);
            String label = formatAxis(max * (lines - i) / lines);
            gc.fillText(label, 10, y + 4);
        }

        drawSeries(gc, new ArrayList<>(downloads), max, left, top, plotW, plotH, DOWNLOAD, true);
        drawSeries(gc, new ArrayList<>(uploads), max, left, top, plotW, plotH, UPLOAD, false);

        gc.setFill(DOWNLOAD);
        gc.fillOval(left, height - 18, 8, 8);
        gc.setFill(Color.web("#D7E3F4"));
        gc.fillText("Download", left + 14, height - 10);
        gc.setFill(UPLOAD);
        gc.fillOval(left + 100, height - 18, 8, 8);
        gc.setFill(Color.web("#D7E3F4"));
        gc.fillText("Upload", left + 114, height - 10);
    }

    private void drawSeries(GraphicsContext gc, List<Double> values, double max,
                            double left, double top, double plotW, double plotH, Color color, boolean fill) {
        if (values.size() < 2) {
            return;
        }
        double step = plotW / (MAX_POINTS - 1.0);
        double startX = left + (MAX_POINTS - values.size()) * step;

        gc.beginPath();
        for (int i = 0; i < values.size(); i++) {
            double x = startX + i * step;
            double y = top + plotH - (values.get(i) / max) * plotH;
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        if (fill) {
            double lastX = startX + (values.size() - 1) * step;
            gc.lineTo(lastX, top + plotH);
            gc.lineTo(startX, top + plotH);
            gc.closePath();
            gc.setFill(new LinearGradient(0, top, 0, top + plotH, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.28)),
                    new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.02))));
            gc.fill();
        } else {
            gc.setStroke(color);
            gc.setLineWidth(2.4);
            gc.stroke();
        }

        gc.beginPath();
        for (int i = 0; i < values.size(); i++) {
            double x = startX + i * step;
            double y = top + plotH - (values.get(i) / max) * plotH;
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.setStroke(color);
        gc.setLineWidth(fill ? 2.6 : 2.2);
        gc.stroke();
    }

    private static double niceMax(double max) {
        if (max <= 1) {
            return 1;
        }
        double exp = Math.pow(10, Math.floor(Math.log10(max)));
        double n = max / exp;
        double nice = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10;
        return nice * exp;
    }

    private static String formatAxis(double value) {
        if (value >= 10) {
            return String.format("%.0f", value);
        }
        return String.format("%.1f", value);
    }
}
