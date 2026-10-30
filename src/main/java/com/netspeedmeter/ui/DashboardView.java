package com.netspeedmeter.ui;

import com.netspeedmeter.model.AdapterInfo;
import com.netspeedmeter.model.PingResult;
import com.netspeedmeter.model.SpeedSample;
import com.netspeedmeter.monitor.MonitorService;
import com.netspeedmeter.monitor.WindowsNetworkSampler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class DashboardView {

    private final BorderPane root = new BorderPane();
    private final MonitorService monitor = new MonitorService();

    private final Label statusDot = new Label("●");
    private final Label statusText = new Label("Stopped");
    private final Label downloadValue = new Label("0.00");
    private final Label uploadValue = new Label("0.00");
    private final Label pingValue = new Label("—");
    private final Label pingDetail = new Label("Waiting for first probe");
    private final Label adapterName = new Label("—");
    private final Label adapterDesc = new Label("Adapter details appear once monitoring starts.");
    private final Label adapterMac = new Label("MAC  —");
    private final Label adapterIp = new Label("IPv4  —");
    private final Label adapterLink = new Label("Link  —");
    private final Label adapterIndex = new Label("Index  —");
    private final Label adapterStatus = new Label("Status  —");
    private final Label totalsLabel = new Label("Counters  RX 0 B   TX 0 B");
    private final Label messageLabel = new Label("Press Start to read live Windows NIC statistics.");
    private final TextField pingHostField = new TextField("8.8.8.8");
    private final Button startButton = new Button("Start monitoring");
    private final Button stopButton = new Button("Stop");
    private final SpeedGraph graph = new SpeedGraph();

    public DashboardView() {
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(22));
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildFooter());

        stopButton.setDisable(true);
        startButton.setOnAction(e -> startMonitoring());
        stopButton.setOnAction(e -> stopMonitoring());

        monitor.setSpeedListener(sample -> Platform.runLater(() -> {
            if (monitor.isRunning()) {
                applySample(sample);
            }
        }));
        monitor.setPingListener(result -> Platform.runLater(() -> {
            if (monitor.isRunning()) {
                applyPing(result);
            }
        }));
        monitor.setErrorListener(message -> Platform.runLater(() -> {
            messageLabel.setText(message);
            messageLabel.getStyleClass().remove("ok-text");
            if (!messageLabel.getStyleClass().contains("error-text")) {
                messageLabel.getStyleClass().add("error-text");
            }
        }));
    }

    public BorderPane getRoot() {
        return root;
    }

    public void shutdown() {
        monitor.close();
    }

    private HBox buildHeader() {
        Label kicker = new Label("WINDOWS NETWORK OPERATIONS");
        kicker.getStyleClass().add("kicker");
        Label title = new Label("NetSpeed Meter");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Live download / upload from adapter byte counters  ·  Ping via ping.exe");
        subtitle.getStyleClass().add("subtitle");

        VBox titles = new VBox(4, kicker, title, subtitle);

        statusDot.getStyleClass().addAll("status-dot", "status-idle");
        statusText.getStyleClass().add("status-text");
        HBox badge = new HBox(8, statusDot, statusText);
        badge.getStyleClass().add("status-badge");
        badge.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        startButton.getStyleClass().add("primary-button");
        stopButton.getStyleClass().add("ghost-button");
        HBox actions = new HBox(10, badge, startButton, stopButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(16, titles, spacer, actions);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 4, 18, 4));
        return header;
    }

    private VBox buildCenter() {
        VBox downloadCard = metricCard("DOWNLOAD", downloadValue, "Mbps", "Bytes received across active adapters", "cyan");
        VBox uploadCard = metricCard("UPLOAD", uploadValue, "Mbps", "Bytes sent across active adapters", "orange");
        VBox pingCard = buildPingCard();
        HBox metrics = new HBox(16, downloadCard, uploadCard, pingCard);
        HBox.setHgrow(downloadCard, Priority.ALWAYS);
        HBox.setHgrow(uploadCard, Priority.ALWAYS);
        HBox.setHgrow(pingCard, Priority.ALWAYS);

        VBox graphCard = new VBox(10);
        graphCard.getStyleClass().add("card");
        Label graphTitle = new Label("LIVE THROUGHPUT");
        graphTitle.getStyleClass().add("card-kicker");
        Label graphHint = new Label("Rolling window of measured Mbps (not simulated)");
        graphHint.getStyleClass().add("muted");
        graph.setMinHeight(280);
        graph.setPrefHeight(320);
        VBox.setVgrow(graph, Priority.ALWAYS);
        graphCard.getChildren().addAll(graphTitle, graphHint, graph);

        VBox adapterCard = buildAdapterCard();
        HBox bottom = new HBox(16, graphCard, adapterCard);
        HBox.setHgrow(graphCard, Priority.ALWAYS);
        adapterCard.setMinWidth(320);
        adapterCard.setPrefWidth(360);
        VBox.setVgrow(bottom, Priority.ALWAYS);

        VBox center = new VBox(16, metrics, bottom);
        VBox.setVgrow(graphCard, Priority.ALWAYS);
        return center;
    }

    private VBox metricCard(String title, Label valueLabel, String unit, String hint, String accent) {
        Label kicker = new Label(title);
        kicker.getStyleClass().addAll("card-kicker", accent);
        valueLabel.getStyleClass().addAll("metric-value", accent);
        Label unitLabel = new Label(unit);
        unitLabel.getStyleClass().add("metric-unit");
        HBox valueRow = new HBox(8, valueLabel, unitLabel);
        valueRow.setAlignment(Pos.BASELINE_LEFT);
        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("muted");
        VBox card = new VBox(8, kicker, valueRow, hintLabel);
        card.getStyleClass().addAll("card", "metric-card");
        return card;
    }

    private VBox buildPingCard() {
        Label kicker = new Label("LATENCY");
        kicker.getStyleClass().addAll("card-kicker", "lime");
        pingValue.getStyleClass().addAll("metric-value", "lime");
        Label unit = new Label("ms");
        unit.getStyleClass().add("metric-unit");
        HBox valueRow = new HBox(8, pingValue, unit);
        valueRow.setAlignment(Pos.BASELINE_LEFT);
        pingDetail.getStyleClass().add("muted");
        pingHostField.getStyleClass().add("host-field");
        pingHostField.setPromptText("Ping host");
        Label hostLabel = new Label("Target");
        hostLabel.getStyleClass().add("field-label");
        VBox card = new VBox(8, kicker, valueRow, pingDetail, hostLabel, pingHostField);
        card.getStyleClass().addAll("card", "metric-card");
        return card;
    }

    private VBox buildAdapterCard() {
        Label kicker = new Label("ACTIVE ADAPTER");
        kicker.getStyleClass().add("card-kicker");
        adapterName.getStyleClass().add("adapter-name");
        adapterDesc.getStyleClass().add("muted");
        adapterDesc.setWrapText(true);
        for (Label row : new Label[]{adapterMac, adapterIp, adapterLink, adapterIndex, adapterStatus, totalsLabel}) {
            row.getStyleClass().add("detail-row");
            row.setWrapText(true);
        }
        GridPane grid = new GridPane();
        grid.setVgap(8);
        grid.add(adapterMac, 0, 0);
        grid.add(adapterIp, 0, 1);
        grid.add(adapterLink, 0, 2);
        grid.add(adapterIndex, 0, 3);
        grid.add(adapterStatus, 0, 4);
        grid.add(totalsLabel, 0, 5);

        VBox card = new VBox(10, kicker, adapterName, adapterDesc, grid);
        card.getStyleClass().add("card");
        return card;
    }

    private HBox buildFooter() {
        messageLabel.getStyleClass().add("footer-message");
        messageLabel.setWrapText(true);
        HBox footer = new HBox(messageLabel);
        footer.setPadding(new Insets(14, 6, 4, 6));
        return footer;
    }

    private void startMonitoring() {
        messageLabel.getStyleClass().remove("error-text");
        graph.clear();
        monitor.setPingHost(pingHostField.getText());
        monitor.start();
        startButton.setDisable(true);
        stopButton.setDisable(false);
        pingHostField.setDisable(true);
        setStatus(true);
        messageLabel.setText("Monitoring Windows interface statistics on a background thread.");
    }

    private void stopMonitoring() {
        monitor.stop();
        startButton.setDisable(false);
        stopButton.setDisable(true);
        pingHostField.setDisable(false);
        setStatus(false);
        messageLabel.setText("Monitoring stopped. Last measured values are held on screen.");
    }

    private void setStatus(boolean live) {
        statusDot.getStyleClass().removeAll("status-live", "status-idle");
        statusDot.getStyleClass().add(live ? "status-live" : "status-idle");
        statusText.setText(live ? "Live" : "Stopped");
    }

    private void applySample(SpeedSample sample) {
        downloadValue.setText(WindowsNetworkSampler.formatMbps(sample.downloadMbps()));
        uploadValue.setText(WindowsNetworkSampler.formatMbps(sample.uploadMbps()));
        if (sample.warning() == null) {
            graph.addSample(sample.downloadMbps(), sample.uploadMbps());
        }
        bindAdapter(sample.activeAdapter());
        if (sample.warning() != null && !sample.warning().isBlank()) {
            messageLabel.getStyleClass().remove("error-text");
            messageLabel.setText(sample.warning());
        } else {
            messageLabel.getStyleClass().remove("error-text");
            int count = sample.adapters().size();
            messageLabel.setText("Sampling " + count + " active adapter" + (count == 1 ? "" : "s")
                    + " · speeds are byte-counter deltas converted to Mbps.");
        }
    }

    private void applyPing(PingResult result) {
        if (result.latencyMs() != null) {
            pingValue.setText(String.valueOf(result.latencyMs()));
        } else {
            pingValue.setText("—");
        }
        pingDetail.setText(result.detail());
    }

    private void bindAdapter(AdapterInfo adapter) {
        adapterName.setText(adapter.name());
        adapterDesc.setText(adapter.description());
        adapterMac.setText("MAC  " + adapter.macAddress());
        adapterIp.setText("IPv4  " + adapter.ipv4());
        adapterLink.setText("Link  " + adapter.linkSpeed());
        adapterIndex.setText("Index  " + (adapter.interfaceIndex() >= 0 ? adapter.interfaceIndex() : "—"));
        adapterStatus.setText("Status  " + adapter.status());
        totalsLabel.setText("Counters  RX " + formatBytes(adapter.receivedBytes())
                + "   TX " + formatBytes(adapter.sentBytes()));
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.1f MB", mb);
        }
        return String.format("%.2f GB", mb / 1024.0);
    }
}
