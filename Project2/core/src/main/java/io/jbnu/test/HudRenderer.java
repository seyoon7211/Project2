package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class HudRenderer implements Disposable {

    private final BitmapFont hudFont;
    private final OrthographicCamera hudCamera;

    public HudRenderer() {
        hudFont = new BitmapFont();
        hudFont.setColor(Color.BLACK);
        hudFont.getData().setScale(1.5f);

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, 800, 600);
    }

    public void draw(SpriteBatch batch, int remainingBlocks,
                     boolean playerMode, int currentStage, int totalStages,
                     boolean paused, boolean gameOver, float endingTimer) {
        batch.setProjectionMatrix(hudCamera.combined);
        batch.setColor(Color.WHITE);
        hudFont.getData().setScale(1.5f);


        if (gameOver) {

            if (endingTimer < 4f) {
                return;
            }

            batch.begin();
            hudFont.setColor(Color.WHITE);

            GlyphLayout title = new GlyphLayout(
                hudFont,
                "ALL STAGES CLEAR!"
            );

            hudFont.draw(
                batch,
                title,
                (hudCamera.viewportWidth - title.width) / 2f,
                340
            );

            GlyphLayout hint = new GlyphLayout(
                hudFont,
                "R: Play Again"
            );

            hudFont.draw(
                batch,
                hint,
                (hudCamera.viewportWidth - hint.width) / 2f,
                280
            );

            batch.end();
            return;
        }

        hudFont.setColor(
            currentStage == 3 ? Color.WHITE : Color.BLACK
        );

        batch.begin();

        hudFont.draw(
            batch,
            "Blocks: " + remainingBlocks + " (SPACE: Place)",
            20, 575
        );

        hudFont.draw(
            batch,
            "Mode: " + (playerMode ? "Game" : "Puzzle")
                + " (X: Switch)",
            20, 540
        );

        hudFont.draw(
            batch,
            "Stage: " + currentStage + " / " + totalStages,
            20, 505
        );

        hudFont.draw(
            batch,
            "ENTER: Pause / Resume   R: Restart",
            20, 470
        );

        if (paused) {
            GlyphLayout pauseText = new GlyphLayout(
                hudFont,
                "PAUSED"
            );

            hudFont.draw(
                batch,
                pauseText,
                (hudCamera.viewportWidth - pauseText.width) / 2f,
                320
            );
        }

        batch.end();
    }

    @Override
    public void dispose() {
        hudFont.dispose();
    }
}
