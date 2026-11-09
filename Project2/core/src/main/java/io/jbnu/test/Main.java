package io.jbnu.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

// 블럭 회수 없애기
/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    private static final float WORLD_WIDTH = 2600;
    private static final float MOVE_SPEED = 260;
    private static final float JUMP_SPEED = 650;
    private static final float GRAVITY = 1700;

    private SpriteBatch batch;
    private Texture playerTexture, flagTexture;

    private Texture block1Texture;
    private Texture block2Texture;
    private Texture block3Texture;
    private Texture blockTexture;
    private OrthographicCamera camera;
    private FitViewport viewport;

    private final Rectangle player = new Rectangle(80, 60, 40, 50);
    private final Rectangle flag = new Rectangle(2370, 344, 150, 100);
    private final Array<Rectangle> blocks = new Array<>();
    private float velocityX, velocityY;
    private boolean grounded;
    private boolean gameOver;
    private boolean paused = false;
    private static final int TOTAL_STAGES = 3;
    private int currentStage = 1;
    private final Rectangle blockItem = new Rectangle(0, 0, 28, 28);
    private boolean blockItemCollected = false;

    private Texture blockItemTexture;
    private float endingTimer = 0f;

    private boolean playerMode = true;
    private ShapeRenderer gridRenderer;
    private static final float GRID_SIZE = 40f; // 격자 한 칸 크기

    private final Rectangle ghost = new Rectangle(0, 0, GRID_SIZE, GRID_SIZE);
    private final Array<Rectangle> placedBlocks = new Array<>();

    private Texture hazardTexture;
    private final Array<Rectangle> hazards = new Array<>();

    // 현재 스테이지의 블록 제한
    private int stageBlockLimit = 5;
    private int remainingBlocks = stageBlockLimit;

    private HudRenderer hudRenderer;

    private void toggleBlock() {
        if (remainingBlocks <= 0) {
            return;
        }

        Rectangle newBlock = new Rectangle(
            ghost.x,
            ghost.y,
            GRID_SIZE,
            GRID_SIZE
        );


        if (newBlock.overlaps(player)) {
            return;
        }


        for (Rectangle block : blocks) {
            if (newBlock.overlaps(block)) {
                return;
            }
        }


        for (Rectangle hazard : hazards) {
            if (newBlock.overlaps(hazard)) {
                return;
            }
        }

        blocks.add(newBlock);
        placedBlocks.add(newBlock);
        remainingBlocks--;
    }

    private void drawGhost() {
        batch.setColor(1f, 1f, 1f, 0.5f);

        batch.draw(
            blockTexture,
            ghost.x,
            ghost.y,
            GRID_SIZE,
            GRID_SIZE
        );

        batch.setColor(Color.WHITE);
    }

    private void drawGrid() {
        float viewWidth = camera.viewportWidth * camera.zoom;
        float viewHeight = camera.viewportHeight * camera.zoom;

        float left = camera.position.x - viewWidth / 2f;
        float right = camera.position.x + viewWidth / 2f;
        float bottom = camera.position.y - viewHeight / 2f;
        float top = camera.position.y + viewHeight / 2f;

        float startX = MathUtils.floor(left / GRID_SIZE) * GRID_SIZE;
        float startY = MathUtils.floor(bottom / GRID_SIZE) * GRID_SIZE;

        gridRenderer.setProjectionMatrix(camera.combined);
        gridRenderer.begin(ShapeRenderer.ShapeType.Line);
        gridRenderer.setColor(0.7f, 0.7f, 0.7f, 1f);

        // 세로선
        for (float x = startX; x <= right; x += GRID_SIZE) {
            gridRenderer.line(x, bottom, x, top);
        }

        // 가로선
        for (float y = startY; y <= top; y += GRID_SIZE) {
            gridRenderer.line(left, y, right, y);
        }

        gridRenderer.end();
    }

    private void toggleMode() {
        playerMode = !playerMode;

        velocityX = 0;
        velocityY = 0;

        if (!playerMode) {
            float centerX = player.x + player.width / 2f;
            float centerY = player.y + player.height / 2f;

            ghost.setPosition(
                MathUtils.floor(centerX / GRID_SIZE) * GRID_SIZE,
                MathUtils.floor(centerY / GRID_SIZE) * GRID_SIZE
            );
        }
    }

    private boolean touchesHazard(Rectangle target) {
        for (Rectangle hazard : hazards) {
            float centerX = hazard.x + hazard.width / 2f;
            float centerY = hazard.y + hazard.height / 2f;

            // 이미지 가장자리보다 조금 작은 충돌 범위
            float radius = Math.min(
                hazard.width,
                hazard.height
            ) * 0.35f;

            float closestX = MathUtils.clamp(
                centerX,
                target.x,
                target.x + target.width
            );

            float closestY = MathUtils.clamp(
                centerY,
                target.y,
                target.y + target.height
            );

            float dx = centerX - closestX;
            float dy = centerY - closestY;

            if (dx * dx + dy * dy <= radius * radius) {
                return true;
            }
        }

        return false;
    }

    private void updateGhost() {
        float nextX = ghost.x;
        float nextY = ghost.y;

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            nextX -= GRID_SIZE;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            nextX += GRID_SIZE;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            nextY += GRID_SIZE;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            nextY -= GRID_SIZE;
        }

        float halfWidth = camera.viewportWidth * camera.zoom / 2f;
        float halfHeight = camera.viewportHeight * camera.zoom / 2f;

        float left = Math.max(0, camera.position.x - halfWidth);
        float right = Math.min(
            WORLD_WIDTH,
            camera.position.x + halfWidth
        );
        float bottom = camera.position.y - halfHeight;
        float top = camera.position.y + halfHeight;

        float minX = MathUtils.ceil(left / GRID_SIZE) * GRID_SIZE;
        float maxX = MathUtils.floor(right / GRID_SIZE) * GRID_SIZE - GRID_SIZE;
        float minY = MathUtils.ceil(bottom / GRID_SIZE) * GRID_SIZE;
        float maxY = MathUtils.floor(top / GRID_SIZE) * GRID_SIZE - GRID_SIZE;

        ghost.x = MathUtils.clamp(nextX, minX, maxX);
        ghost.y = MathUtils.clamp(nextY, minY, maxY);
    }

    private void loadStage(int stage) {
        currentStage = stage;
        endingTimer = 0f;
        blocks.clear();
        hazards.clear();
        placedBlocks.clear();
        velocityX = 0;
        velocityY = 0;
        grounded = false;
        gameOver = false;
        paused = false;
        playerMode = true;

        stageBlockLimit = 5;
        remainingBlocks = stageBlockLimit;
        blockItemCollected = false;

        player.setPosition(80, 60);
        ghost.setPosition(0, 0);

        switch (stage) {
            case 1:
                blockTexture = block1Texture;
                stageBlockLimit = 5;
                blocks.add(new Rectangle(0, 0, 360, 60));
                blocks.add(new Rectangle(560, 60, 160, 24));
                blocks.add(new Rectangle(960, 140, 120, 24));
                blocks.add(new Rectangle(1360, 60, 280, 24));
                blocks.add(new Rectangle(1880, 160, 120, 24));
                blocks.add(new Rectangle(2280, 260, 280, 24));

                blockItem.setPosition(620, 84);
                hazards.add(new Rectangle(1460, 84, 48, 48));
                flag.set(2370, 284, 150, 100);
                break;

            case 2:
                blockTexture = block2Texture;
                stageBlockLimit = 5;
                blocks.add(new Rectangle(0, 0, 320, 60));
                blocks.add(new Rectangle(520, 160, 120, 24));
                blocks.add(new Rectangle(880, 40, 160, 24));
                blocks.add(new Rectangle(1440, 80, 80, 24));
                blocks.add(new Rectangle(1760, 240, 120, 24));
                blocks.add(new Rectangle(2080, 120, 120, 24));
                blocks.add(new Rectangle(2360, 300, 200, 24));

                hazards.add(new Rectangle(980, 64, 48, 48));
                hazards.add(new Rectangle(1240, 160, 56, 56));
                blockItem.setPosition(2040, 180);
                flag.set(2380, 324, 150, 100);
                break;

            case 3:
                blockTexture = block3Texture;
                stageBlockLimit = 5;

                blocks.add(new Rectangle(0, 0, 280, 60));
                blocks.add(new Rectangle(560, 100, 80, 40));
                blocks.add(new Rectangle(920, 280, 240, 40));
                blocks.add(new Rectangle(1480, 40, 80, 40));
                blocks.add(new Rectangle(1840, 200, 80, 40));
                blocks.add(new Rectangle(2320, 380, 240, 24));

                blockItem.setPosition(1300, 150);

                hazards.add(new Rectangle(1060, 320, 48, 48));
                hazards.add(new Rectangle(1400, 150, 56, 56));
                hazards.add(new Rectangle(2120, 300, 56, 56));

                flag.set(2370, 404, 150, 100);
                break;


        }

        remainingBlocks = stageBlockLimit;

        camera.position.set(400, 300, 0);
        camera.update();
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        playerTexture = new Texture(Gdx.files.internal("player.png"));
        block1Texture = new Texture(Gdx.files.internal("block1.png"));
        block2Texture = new Texture(Gdx.files.internal("block2.png"));
        block3Texture = new Texture(Gdx.files.internal("block3.png"));
        hazardTexture = new Texture(Gdx.files.internal("hazard_drone.png"));
        blockItemTexture = new Texture(Gdx.files.internal("item.png"));
        flagTexture = new Texture(Gdx.files.internal("flag2.png"));

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        viewport = new FitViewport(800, 600, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        gridRenderer = new ShapeRenderer();

        loadStage(1);

        hudRenderer = new HudRenderer();
    }

    @Override
    public void render() {
        // 큰 프레임 시간으로 인해 한 번에 너무 멀리 이동하는 것을 제한합니다.
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        update(dt);

        switch (currentStage) {
            case 1:
                ScreenUtils.clear(195f / 255f, 232f / 255f, 224f / 255f, 1f);
                break;

            case 2:
                ScreenUtils.clear(112f / 255f, 183f / 255f, 237f / 255f, 1f);
                break;

            case 3:
                ScreenUtils.clear(16f / 255f, 19f / 255f, 41f / 255f, 1f);
                break;
        }

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (Rectangle block : blocks) {
            batch.draw(blockTexture, block.x, block.y, block.width, block.height);
        }

        batch.draw(flagTexture, flag.x, flag.y, flag.width, flag.height);

        if (!blockItemCollected) {
            batch.setColor(Color.WHITE);
            batch.draw(
                blockItemTexture,
                blockItem.x, blockItem.y,
                blockItem.width, blockItem.height
            );
        }

        batch.draw(playerTexture, player.x, player.y, player.width, player.height);
        batch.setColor(Color.WHITE);

        for (Rectangle hazard : hazards) {
            batch.draw(
                hazardTexture,
                hazard.x,
                hazard.y,
                hazard.width,
                hazard.height
            );
        }

        batch.end();

        if (!playerMode) {
            drawGrid();
            batch.begin();
            drawGhost();
            batch.end();
        }

        hudRenderer.draw(
            batch, remainingBlocks, playerMode, currentStage,
            TOTAL_STAGES, paused, gameOver, endingTimer
        );
    }

    private void restartGame() {
        loadStage(currentStage);
    }

    private void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if (gameOver) {
                currentStage = 1;
            }
            restartGame();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            paused = !paused;
            return;
        }

        if (paused) {
            return;
        }

        if (gameOver) {
            endingTimer += dt;

            if (endingTimer < 4f) {
                camera.position.y += 150f * dt;
                camera.update();
            }

            return;
        }

        if (dt <= 0) {
            return;
        }

        // X를 한 번 누를 때마다 모드 전환
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            toggleMode();
        }

        if (!playerMode) {
            updateGhost();

            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                toggleBlock();
            }

            return;
        }

        // 1. 좌우 이동과 지상 점프
        velocityX = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            velocityX -= MOVE_SPEED;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            velocityX += MOVE_SPEED;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && grounded) {
            velocityY = JUMP_SPEED;
            grounded = false;
        }

        // 2. 중력: 아래쪽 속도는 음수이며 최대 낙하 속도를 제한합니다.
        velocityY = Math.max(velocityY - GRAVITY * dt, -900);

        // 3. X축과 Y축을 나누어 이동하고 충돌을 보정합니다.
        moveX(dt);
        moveY(dt);

        if (touchesHazard(player)) {
            restartGame();
            return;
        }

        if (!blockItemCollected && player.overlaps(blockItem)) {
            blockItemCollected = true;
            remainingBlocks++;
        }

        // 4. 낭떠러지에 떨어지면 시작점으로 복귀합니다.
        if (player.y < -100) {
            restartGame();
            return;
        }

        if (player.overlaps(flag)) {
            if (currentStage < TOTAL_STAGES) {
                loadStage(currentStage + 1);
            } else {
                gameOver = true;
                velocityX = 0;
                velocityY = 0;
            }

            return;
        }


        camera.position.x = MathUtils.clamp(
            player.x + player.width / 2,
            400,
            WORLD_WIDTH - 400
        );
        camera.update();
    }

    private void moveX(float dt) {
        player.x += velocityX * dt;

        for (Rectangle block : blocks) {
            if (!player.overlaps(block)) {
                continue;
            }
            if (velocityX > 0) {
                player.x = block.x - player.width;
            } else if (velocityX < 0) {
                player.x = block.x + block.width;
            }
        }

        player.x = MathUtils.clamp(player.x, 0, WORLD_WIDTH - player.width);
    }

    private void moveY(float dt) {
        float dy = velocityY * dt;
        player.y += dy;
        grounded = false;

        for (Rectangle block : blocks) {
            if (!player.overlaps(block)) {
                continue;
            }
            if (dy < 0) { // 내려오던 중: 블록 위에 착지
                player.y = block.y + block.height;
                velocityY = 0;
                grounded = true;
            } else if (dy > 0) { // 올라가던 중: 블록 아래에 머리 충돌
                player.y = block.y - player.height;
                velocityY = 0;
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void dispose() {
        batch.dispose();
        playerTexture.dispose();
        block1Texture.dispose();
        block2Texture.dispose();
        block3Texture.dispose();
        flagTexture.dispose();
        hudRenderer.dispose();
        blockItemTexture.dispose();
        gridRenderer.dispose();
        hazardTexture.dispose();
    }
}
