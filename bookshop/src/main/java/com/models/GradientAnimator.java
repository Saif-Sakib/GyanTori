package com.models;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.layout.Region;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

public class GradientAnimator {
    private Timeline timeline;
    private final Region target;
    private double offset = 0;

    public GradientAnimator(Region target) {
        this.target = target;
        setupAnimation();
    }

    private void setupAnimation() {
        timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(target.translateXProperty(), 0)),
                new KeyFrame(Duration.seconds(3), new KeyValue(target.translateXProperty(), -100)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);

        timeline.setOnFinished(event -> {
            offset = (offset + 0.1) % 1.0;
            updateGradient();
        });
    }

    private void updateGradient() {
        Stop[] stops = {
                new Stop(0, javafx.scene.paint.Color.rgb(202, 99, 202)),
                new Stop(0.3, javafx.scene.paint.Color.rgb(171, 65, 171)),
                new Stop(0.6, javafx.scene.paint.Color.rgb(224, 124, 61)),
                new Stop(1, javafx.scene.paint.Color.rgb(202, 99, 202))
        };

        LinearGradient gradient = new LinearGradient(
                offset, 0, offset + 1, 0, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                stops);

        target.setStyle("-fx-background-color: " + gradient.toString() + ";");
    }

    public void start() {
        timeline.play();
    }

    public void stop() {
        timeline.stop();
    }
}