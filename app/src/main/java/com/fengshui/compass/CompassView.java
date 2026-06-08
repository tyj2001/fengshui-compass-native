package com.fengshui.compass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * 专业风水罗盘盘面绘制View
 * 
 * 盘面结构（从外到内）：
 * 1. 外圈装饰
 * 2. 二十八星宿（28 Lunar Mansions）— 最外圈
 * 3. 一百二十分金（120 Golden Points）
 * 4. 六十甲子（60 Sexagenary Cycle）
 * 5. 天盘缝针（24山 +7.5° 缝针）
 * 6. 人盘中针（24山 -7.5° 中针）
 * 7. 地盘正针（24山 0° 正针）
 * 8. 六十四卦（64 Hexagrams）
 * 9. 后天八卦（8 Trigrams）
 * 10. 360度刻度
 * 11. 指针系统
 */
public class CompassView extends View {

    private float azimuth = 0f; // 当前方位角度

    // 颜色定义
    private static final int COLOR_BG = 0xFF1A1A2E;
    private static final int COLOR_RING_OUTER = 0xFFD4A574;
    private static final int COLOR_RING_INNER = 0xFF2D2D44;
    private static final int COLOR_TEXT_GOLD = 0xFFD4A574;
    private static final int COLOR_TEXT_RED = 0xFFE74C3C;
    private static final int COLOR_TEXT_GREEN = 0xFF27AE60;
    private static final int COLOR_TEXT_BLUE = 0xFF3498DB;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DARK = 0xFFA09070;
    private static final int COLOR_NEEDLE_RED = 0xFFE74C3C;
    private static final int COLOR_NEEDLE_BLACK = 0xFF2C3E50;
    private static final int COLOR_CENTER_DOT = 0xFFD4A574;
    private static final int COLOR_TICK = 0xFFD4A574;
    private static final int COLOR_BAGUA_BG = 0xFF3D3D55;
    private static final int COLOR_HEXAGRAM_LINE = 0xFF8B7355;
    private static final int COLOR_HEXAGRAM_BG = 0xFF2A2A40;

    private Paint paint;
    private Paint textPaint;
    private Paint tickPaint;
    private Paint needlePaint;
    private Paint baguaPaint;
    private Paint hexagramPaint;
    private Paint hexagramBgPaint;
    private Paint sectorPaint;

    private float centerX, centerY, radius;
    private float density;

    // ========== 24山数据 ==========
    private static final String[] MOUNTAINS_24 = {
        "子", "癸", "丑", "艮", "寅", "甲",
        "卯", "乙", "辰", "巽", "巳", "丙",
        "午", "丁", "未", "坤", "申", "庚",
        "酉", "辛", "戌", "乾", "亥", "壬"
    };

    // 24山对应的角度（中心角度）
    private static final float[] MOUNTAIN_ANGLES = {
        0f, 15f, 30f, 45f, 60f, 75f,
        90f, 105f, 120f, 135f, 150f, 165f,
        180f, 195f, 210f, 225f, 240f, 255f,
        270f, 285f, 300f, 315f, 330f, 345f
    };

    // ========== 后天八卦 ==========
    private static final String[] BAGUA = {
        "坎", "艮", "震", "巽", "离", "坤", "兑", "乾"
    };
    private static final float[] BAGUA_ANGLES = {
        0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f
    };

    // ========== 二十八星宿 ==========
    // 顺序：从角宿（约东偏南）开始，按逆时针排列
    // 实际罗盘中二十八星宿对应24山方位
    // 角(辰/巽之间) 亢(辰) 氐(卯/辰之间) 房(卯) 心(卯/寅之间) 尾(寅) 箕(寅/丑之间)
    // 斗(丑) 牛(丑/子之间) 女(子) 虚(子/亥之间) 危(亥) 室(亥/乾之间) 壁(乾)
    // 奎(戌/乾之间) 娄(戌) 胃(酉/戌之间) 昴(酉) 毕(酉/申之间) 觜(申) 参(申/坤之间)
    // 井(未/坤之间) 鬼(未) 柳(午/未之间) 星(午) 张(午/巳之间) 翼(巳) 轸(巳/辰之间)
    private static final String[] XIU_28 = {
        "角", "亢", "氐", "房", "心", "尾", "箕",
        "斗", "牛", "女", "虚", "危", "室", "壁",
        "奎", "娄", "胃", "昴", "毕", "觜", "参",
        "井", "鬼", "柳", "星", "张", "翼", "轸"
    };

    // 二十八星宿对应的中心角度（度），从正北顺时针
    // 每个星宿占约12.857度（360/28）
    private static final float[] XIU_ANGLES = new float[28];
    static {
        // 角宿起始于约112.5度（辰宫中间偏左）
        // 按每个星宿约12.857度排列
        float startAngle = 112.5f;
        float step = 360f / 28f;
        for (int i = 0; i < 28; i++) {
            float angle = startAngle + i * step;
            if (angle >= 360) angle -= 360;
            XIU_ANGLES[i] = angle;
        }
    }

    // 二十八星宿五行属性
    private static final String[] XIU_WUXING = {
        "木", "金", "土", "日", "月", "火", "水",  // 东方青龙七宿
        "木", "金", "土", "日", "月", "火", "水",  // 北方玄武七宿
        "木", "金", "土", "日", "月", "火", "水",  // 西方白虎七宿
        "木", "金", "土", "日", "月", "火", "水"   // 南方朱雀七宿
    };

    // 二十八星宿所属七曜（星期）
    private static final String[] XIU_QIYAO = {
        "日", "月", "火", "水", "木", "金", "土",  // 角亢氐房心尾箕
        "日", "月", "火", "水", "木", "金", "土",  // 斗牛女虚危室壁
        "日", "月", "火", "水", "木", "金", "土",  // 奎娄胃昴毕觜参
        "日", "月", "火", "水", "木", "金", "土"   // 井鬼柳星张翼轸
    };

    // ========== 六十甲子 ==========
    private static final String[] SEXAGENARY = {
        "甲子", "乙丑", "丙寅", "丁卯", "戊辰", "己巳", "庚午", "辛未", "壬申", "癸酉",
        "甲戌", "乙亥", "丙子", "丁丑", "戊寅", "己卯", "庚辰", "辛巳", "壬午", "癸未",
        "甲申", "乙酉", "丙戌", "丁亥", "戊子", "己丑", "庚寅", "辛卯", "壬辰", "癸巳",
        "甲午", "乙未", "丙申", "丁酉", "戊戌", "己亥", "庚子", "辛丑", "壬寅", "癸卯",
        "甲辰", "乙巳", "丙午", "丁未", "戊申", "己酉", "庚戌", "辛亥", "壬子", "癸丑",
        "甲寅", "乙卯", "丙辰", "丁巳", "戊午", "己未", "庚申", "辛酉", "壬戌", "癸亥"
    };

    // ========== 一百二十分金 ==========
    // 120个分金位（每个3度），只标注有意义的
    // 实际罗盘上只标注部分分金（如丙午丁等）
    private static final String[] FENJIN_120 = new String[120];

    // 天干分金配对（用于一百二十分金）
    private static final String[] FENJIN_TIANGAN = {
        "甲", "丙", "戊", "庚", "壬"  // 阳干
    };
    private static final String[] FENJIN_TIANGAN_YIN = {
        "乙", "丁", "己", "辛", "癸"  // 阴干
    };

    // ========== 六十四卦 ==========
    // 六十四卦名称
    private static final String[] HEXAGRAM_64 = {
        "乾", "坤", "屯", "蒙", "需", "讼", "师", "比",
        "小畜", "履", "泰", "否", "同人", "大有", "谦", "豫",
        "随", "蛊", "临", "观", "噬嗑", "贲", "剥", "复",
        "无妄", "大畜", "颐", "大过", "坎", "离",
        "咸", "恒", "遁", "大壮", "晋", "明夷", "家人", "睽",
        "蹇", "解", "损", "益", "夬", "姤", "萃", "升",
        "困", "井", "革", "鼎", "震", "艮", "渐", "归妹",
        "丰", "旅", "巽", "兑", "涣", "节", "中孚", "小过",
        "既济", "未济"
    };

    // 六十四卦在罗盘中的方位角度（从正北顺时针）
    // 每卦占5.625度
    private static final float[] HEX_ANGLES = new float[64];
    static {
        for (int i = 0; i < 64; i++) {
            HEX_ANGLES[i] = i * (360f / 64f);
        }
    }

    public CompassView(Context context) {
        super(context);
        init();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        baguaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        baguaPaint.setStyle(Paint.Style.FILL);

        hexagramPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hexagramPaint.setStyle(Paint.Style.STROKE);
        hexagramPaint.setStrokeWidth(2 * density);

        hexagramBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hexagramBgPaint.setStyle(Paint.Style.FILL);

        sectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectorPaint.setStyle(Paint.Style.FILL);

        // 初始化一百二十分金
        initFenjin();
    }

    /**
     * 初始化一百二十分金数据
     */
    private void initFenjin() {
        for (int i = 0; i < 120; i++) {
            // 120分金对应360度，每3度一个分金
            // 实际罗盘只标注特定分金位
            // 分金用天干表示：甲丙戊庚壬（阳）乙丁己辛癸（阴）
            int tianGanIndex = i % 10;
            if (tianGanIndex < 5) {
                // 阳干
                FENJIN_120[i] = FENJIN_TIANGAN[tianGanIndex];
            } else {
                // 阴干
                FENJIN_120[i] = FENJIN_TIANGAN_YIN[tianGanIndex - 5];
            }
        }
    }

    /**
     * 更新方位角
     */
    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        float padding = 10 * density;
        radius = Math.min(centerX, centerY) - padding;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 背景
        canvas.drawColor(COLOR_BG);

        // 绘制罗盘（盘面不动，指针旋转）
        // 从外到内绘制各圈层

        // 1. 外圈装饰
        drawOuterRing(canvas);

        // 2. 二十八星宿（最外圈）
        draw28Xiu(canvas);

        // 3. 一百二十分金
        draw120Fenjin(canvas);

        // 4. 六十甲子
        draw60Sexagenary(canvas);

        // 5. 天盘缝针（+7.5°）
        drawTianPan(canvas);

        // 6. 人盘中针（-7.5°）
        drawRenPan(canvas);

        // 7. 地盘正针（24山 0°）
        drawDiPan(canvas);

        // 8. 六十四卦
        draw64Hexagram(canvas);

        // 9. 后天八卦
        drawBagua(canvas);

        // 10. 360度刻度
        drawDegreeTicks(canvas);

        // 11. 中心太极圈
        drawCenterCircle(canvas);

        // 12. 指针
        drawNeedle(canvas);

        // 13. 方位信息
        drawDirectionText(canvas);
    }

    // ========================================================================
    // 1. 外圈装饰
    // ========================================================================
    private void drawOuterRing(Canvas canvas) {
        // 最外圈粗金边
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4 * density);
        paint.setColor(COLOR_RING_OUTER);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // 第二圈细金边
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(COLOR_RING_INNER);
        canvas.drawCircle(centerX, centerY, radius - 2 * density, paint);
    }

    // ========================================================================
    // 2. 二十八星宿
    // ========================================================================
    private void draw28Xiu(Canvas canvas) {
        float ringWidth = 28 * density;
        float outerR = radius - 4 * density;
        float innerR = outerR - ringWidth;

        // 绘制星宿背景扇区（每个星宿一个扇区）
        float stepAngle = 360f / 28f;
        for (int i = 0; i < 28; i++) {
            float startAngle = XIU_ANGLES[i] - stepAngle / 2f;
            float sweepAngle = stepAngle;

            // 交替背景色
            if (i % 2 == 0) {
                sectorPaint.setColor(0xFF252540);
            } else {
                sectorPaint.setColor(0xFF2A2A48);
            }

            RectF rect = new RectF(
                centerX - outerR, centerY - outerR,
                centerX + outerR, centerY + outerR
            );
            canvas.drawArc(rect, -90 + startAngle, sweepAngle, true, sectorPaint);
        }

        // 绘制分隔线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.5f * density);
        paint.setColor(0xFF3D3D55);
        for (int i = 0; i < 28; i++) {
            float angle = (float) Math.toRadians(XIU_ANGLES[i] - stepAngle / 2f);
            float sx = centerX + innerR * (float) Math.sin(angle);
            float sy = centerY - innerR * (float) Math.cos(angle);
            float ex = centerX + outerR * (float) Math.sin(angle);
            float ey = centerY - outerR * (float) Math.cos(angle);
            canvas.drawLine(sx, sy, ex, ey, paint);
        }

        // 绘制星宿名称和五行
        float textR = (outerR + innerR) / 2f;
        for (int i = 0; i < 28; i++) {
            float rad = (float) Math.toRadians(XIU_ANGLES[i]);

            // 星宿名称（大号字）
            textPaint.setTextSize(13 * density);
            textPaint.setColor(COLOR_TEXT_GOLD);
            float tx = centerX + (textR - 2 * density) * (float) Math.sin(rad);
            float ty = centerY - (textR - 2 * density) * (float) Math.cos(rad) + 5 * density;
            canvas.drawText(XIU_28[i], tx, ty, textPaint);

            // 七曜标注（小号字，在名称下方偏外）
            textPaint.setTextSize(8 * density);
            textPaint.setColor(COLOR_TEXT_DARK);
            float qx = centerX + (textR + 8 * density) * (float) Math.sin(rad);
            float qy = centerY - (textR + 8 * density) * (float) Math.cos(rad) + 3 * density;
            canvas.drawText(XIU_QIYAO[i], qx, qy, textPaint);

            // 五行标注（小号字，在名称下方偏内）
            textPaint.setColor(0xFF6B8E6B);
            float wx = centerX + (textR - 10 * density) * (float) Math.sin(rad);
            float wy = centerY - (textR - 10 * density) * (float) Math.cos(rad) + 3 * density;
            canvas.drawText(XIU_WUXING[i], wx, wy, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0xFF3D3D55);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 3. 一百二十分金
    // ========================================================================
    private void draw120Fenjin(Canvas canvas) {
        float ringWidth = 16 * density;
        float outerR = radius - 34 * density;
        float innerR = outerR - ringWidth;

        // 120个分金位，每3度一个
        float step = 3f;

        // 绘制分金文字（只标注有意义的分金位）
        float textR = (outerR + innerR) / 2f;
        textPaint.setTextSize(8 * density);

        for (int i = 0; i < 120; i++) {
            float angleDeg = i * step; // 从0度开始
            float rad = (float) Math.toRadians(angleDeg);

            // 只显示部分分金（避免太拥挤）
            // 每15度（一个山位）显示5个分金中的部分
            // 实际罗盘通常只标注"丙午丁"等
            int posInSector = i % 5;

            // 只标注中间3个分金（跳过首尾）
            if (posInSector == 0 || posInSector == 4) continue;

            String text = FENJIN_120[i];

            // 阳干金色，阴干暗色
            if (posInSector == 1 || posInSector == 3) {
                // 阳干
                textPaint.setColor(COLOR_TEXT_GOLD);
            } else {
                // 阴干
                textPaint.setColor(COLOR_TEXT_DARK);
            }

            float tx = centerX + textR * (float) Math.sin(rad);
            float ty = centerY - textR * (float) Math.cos(rad) + 3 * density;
            canvas.drawText(text, tx, ty, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0xFF3D3D55);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 4. 六十甲子
    // ========================================================================
    private void draw60Sexagenary(Canvas canvas) {
        float ringWidth = 18 * density;
        float outerR = radius - 52 * density;
        float innerR = outerR - ringWidth;

        // 60甲子对应360度，每个占6度
        float step = 360f / 60f;
        float textR = (outerR + innerR) / 2f;
        textPaint.setTextSize(10 * density);

        for (int i = 0; i < 60; i++) {
            float angleDeg = i * step;
            float rad = (float) Math.toRadians(angleDeg);

            // 甲（天干之首）用红色
            if (SEXAGENARY[i].startsWith("甲")) {
                textPaint.setColor(COLOR_TEXT_RED);
            } else if (SEXAGENARY[i].startsWith("庚") || SEXAGENARY[i].startsWith("丙")) {
                textPaint.setColor(COLOR_TEXT_GOLD);
            } else {
                textPaint.setColor(COLOR_TEXT_DARK);
            }

            float tx = centerX + textR * (float) Math.sin(rad);
            float ty = centerY - textR * (float) Math.cos(rad) + 4 * density;
            canvas.drawText(SEXAGENARY[i], tx, ty, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0xFF3D3D55);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 5. 天盘缝针（24山 +7.5°）
    // ========================================================================
    private void drawTianPan(Canvas canvas) {
        float ringWidth = 18 * density;
        float outerR = radius - 72 * density;
        float innerR = outerR - ringWidth;

        float textR = (outerR + innerR) / 2f;

        // 绘制"天盘"标注
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF1A1A2E);
        // 在正北位置画一个半透明背景
        textPaint.setTextSize(9 * density);
        textPaint.setColor(COLOR_TEXT_BLUE);
        float labelRad = (float) Math.toRadians(180);
        float lx = centerX + (textR - 8 * density) * (float) Math.sin(labelRad);
        float ly = centerY - (textR - 8 * density) * (float) Math.cos(labelRad) + 3 * density;
        canvas.drawText("缝", lx, ly, textPaint);

        // 天盘24山（+7.5°偏移）
        textPaint.setTextSize(13 * density);
        for (int i = 0; i < 24; i++) {
            // 天盘缝针比地盘正针顺时针偏移7.5度
            float angleDeg = MOUNTAIN_ANGLES[i] + 7.5f;
            if (angleDeg >= 360) angleDeg -= 360;
            float rad = (float) Math.toRadians(angleDeg);

            // 子午卯酉红色
            if (i == 0 || i == 6 || i == 12 || i == 18) {
                textPaint.setColor(COLOR_TEXT_RED);
            } else {
                textPaint.setColor(COLOR_TEXT_BLUE);
            }

            float tx = centerX + textR * (float) Math.sin(rad);
            float ty = centerY - textR * (float) Math.cos(rad) + 5 * density;
            canvas.drawText(MOUNTAINS_24[i], tx, ty, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0xFF3D3D55);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 6. 人盘中针（24山 -7.5°）
    // ========================================================================
    private void drawRenPan(Canvas canvas) {
        float ringWidth = 18 * density;
        float outerR = radius - 92 * density;
        float innerR = outerR - ringWidth;

        float textR = (outerR + innerR) / 2f;

        // 绘制"人盘"标注
        textPaint.setTextSize(9 * density);
        textPaint.setColor(COLOR_TEXT_GREEN);
        float labelRad = (float) Math.toRadians(180);
        float lx = centerX + (textR - 8 * density) * (float) Math.sin(labelRad);
        float ly = centerY - (textR - 8 * density) * (float) Math.cos(labelRad) + 3 * density;
        canvas.drawText("中", lx, ly, textPaint);

        // 人盘24山（-7.5°偏移）
        textPaint.setTextSize(13 * density);
        for (int i = 0; i < 24; i++) {
            // 人盘中针比地盘正针逆时针偏移7.5度
            float angleDeg = MOUNTAIN_ANGLES[i] - 7.5f;
            if (angleDeg < 0) angleDeg += 360;
            float rad = (float) Math.toRadians(angleDeg);

            // 子午卯酉红色
            if (i == 0 || i == 6 || i == 12 || i == 18) {
                textPaint.setColor(COLOR_TEXT_RED);
            } else {
                textPaint.setColor(COLOR_TEXT_GREEN);
            }

            float tx = centerX + textR * (float) Math.sin(rad);
            float ty = centerY - textR * (float) Math.cos(rad) + 5 * density;
            canvas.drawText(MOUNTAINS_24[i], tx, ty, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0xFF3D3D55);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 7. 地盘正针（24山 0°）
    // ========================================================================
    private void drawDiPan(Canvas canvas) {
        float ringWidth = 20 * density;
        float outerR = radius - 112 * density;
        float innerR = outerR - ringWidth;

        float textR = (outerR + innerR) / 2f;
        textPaint.setTextSize(15 * density);

        for (int i = 0; i < 24; i++) {
            float angleDeg = MOUNTAIN_ANGLES[i];
            float rad = (float) Math.toRadians(angleDeg);

            // 子午卯酉（正北正南正东正西）用红色
            if (i == 0 || i == 6 || i == 12 || i == 18) {
                textPaint.setColor(COLOR_TEXT_RED);
                textPaint.setTextSize(17 * density);
            } else {
                textPaint.setColor(COLOR_TEXT_GOLD);
                textPaint.setTextSize(15 * density);
            }

            float tx = centerX + textR * (float) Math.sin(rad);
            float ty = centerY - textR * (float) Math.cos(rad) + 6 * density;
            canvas.drawText(MOUNTAINS_24[i], tx, ty, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0xFF3D3D55);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 8. 六十四卦
    // ========================================================================
    private void draw64Hexagram(Canvas canvas) {
        float ringWidth = 22 * density;
        float outerR = radius - 134 * density;
        float innerR = outerR - ringWidth;

        // 六十四卦每卦占5.625度
        float step = 360f / 64f;

        // 绘制卦名
        float textR = (outerR + innerR) / 2f;
        textPaint.setTextSize(10 * density);

        for (int i = 0; i < 64; i++) {
            float angleDeg = HEX_ANGLES[i];
            float rad = (float) Math.toRadians(angleDeg);

            // 八纯卦（乾坎艮震巽离坤兑）用红色
            boolean isPure = isPureHexagram(HEXAGRAM_64[i]);
            if (isPure) {
                textPaint.setColor(COLOR_TEXT_RED);
            } else {
                textPaint.setColor(COLOR_TEXT_GOLD);
            }

            float tx = centerX + textR * (float) Math.sin(rad);
            float ty = centerY - textR * (float) Math.cos(rad) + 4 * density;
            canvas.drawText(HEXAGRAM_64[i], tx, ty, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0xFF3D3D55);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    /**
     * 判断是否为八纯卦
     */
    private boolean isPureHexagram(String name) {
        return name.equals("乾") || name.equals("坤") || name.equals("坎") ||
               name.equals("离") || name.equals("震") || name.equals("艮") ||
               name.equals("巽") || name.equals("兑");
    }

    // ========================================================================
    // 9. 后天八卦
    // ========================================================================
    private void drawBagua(Canvas canvas) {
        float baguaR = radius - 160 * density;
        textPaint.setTextSize(16 * density);

        for (int i = 0; i < BAGUA.length; i++) {
            float rad = (float) Math.toRadians(BAGUA_ANGLES[i]);
            float bx = centerX + baguaR * (float) Math.sin(rad);
            float by = centerY - baguaR * (float) Math.cos(rad);

            // 八卦背景圆
            float dotR = 14 * density;
            baguaPaint.setColor(COLOR_BAGUA_BG);
            canvas.drawCircle(bx, by, dotR, baguaPaint);

            // 八卦文字颜色
            if (i == 0) textPaint.setColor(Color.BLACK);      // 坎(水)=黑
            else if (i == 4) textPaint.setColor(COLOR_TEXT_RED); // 离(火)=红
            else textPaint.setColor(COLOR_TEXT_GOLD);

            canvas.drawText(BAGUA[i], bx, by + 6 * density, textPaint);
        }

        // 八卦外圈
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0xFF3D3D55);
        canvas.drawCircle(centerX, centerY, baguaR + 18 * density, paint);
        canvas.drawCircle(centerX, centerY, baguaR - 18 * density, paint);
    }

    // ========================================================================
    // 10. 360度刻度
    // ========================================================================
    private void drawDegreeTicks(Canvas canvas) {
        float outerR = radius - 8 * density;
        float innerR;

        for (int i = 0; i < 360; i++) {
            float angle = (float) Math.toRadians(i);

            if (i % 15 == 0) {
                // 15度（24山刻度）— 长线
                tickPaint.setStrokeWidth(2.5f * density);
                tickPaint.setColor(COLOR_TEXT_GOLD);
                innerR = radius - 28 * density;
            } else if (i % 5 == 0) {
                // 5度 — 中线
                tickPaint.setStrokeWidth(1.5f * density);
                tickPaint.setColor(0xFFA08060);
                innerR = radius - 20 * density;
            } else {
                // 1度 — 短线
                tickPaint.setStrokeWidth(0.8f * density);
                tickPaint.setColor(0xFF605040);
                innerR = radius - 14 * density;
            }

            float startX = centerX + outerR * (float) Math.sin(angle);
            float startY = centerY - outerR * (float) Math.cos(angle);
            float endX = centerX + innerR * (float) Math.sin(angle);
            float endY = centerY - innerR * (float) Math.cos(angle);

            canvas.drawLine(startX, startY, endX, endY, tickPaint);
        }

        // 360度数字标注（每30度）
        textPaint.setTextSize(10 * density);
        textPaint.setColor(COLOR_TEXT_GOLD);
        float textR = radius - 36 * density;
        for (int i = 0; i < 360; i += 30) {
            float rad = (float) Math.toRadians(i);
            float tx = centerX + textR * (float) Math.sin(rad);
            float ty = centerY - textR * (float) Math.cos(rad) + 4 * density;
            canvas.drawText(String.valueOf(i), tx, ty, textPaint);
        }

        // 四正方向标注（子午卯酉/北南东西）
        textPaint.setTextSize(11 * density);
        String[] cardinal = {"北", "东", "南", "西"};
        float[] cardinalAngles = {0f, 90f, 180f, 270f};
        float cardR = radius - 44 * density;
        for (int i = 0; i < 4; i++) {
            float rad = (float) Math.toRadians(cardinalAngles[i]);
            textPaint.setColor(i == 2 ? COLOR_TEXT_RED : COLOR_TEXT_GOLD);
            float tx = centerX + cardR * (float) Math.sin(rad);
            float ty = centerY - cardR * (float) Math.cos(rad) + 4 * density;
            canvas.drawText(cardinal[i], tx, ty, textPaint);
        }
    }

    // ========================================================================
    // 11. 中心太极圈
    // ========================================================================
    private void drawCenterCircle(Canvas canvas) {
        float centerR = 16 * density;

        // 外圈
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * density);
        paint.setColor(COLOR_RING_OUTER);
        canvas.drawCircle(centerX, centerY, centerR + 4 * density, paint);

        // 内圆背景
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF2D2D44);
        canvas.drawCircle(centerX, centerY, centerR, paint);

        // 中心小圆点
        paint.setColor(COLOR_CENTER_DOT);
        canvas.drawCircle(centerX, centerY, 3 * density, paint);
    }

    // ========================================================================
    // 12. 指针系统
    // ========================================================================
    private void drawNeedle(Canvas canvas) {
        float needleLen = radius - 160 * density;
        float needleBase = 16 * density;
        float needleWidth = 7 * density;

        canvas.save();
        // 指针随方位角旋转
        canvas.rotate(azimuth, centerX, centerY);

        // 红色指针（南 — 指向当前方位）
        needlePaint.setColor(COLOR_NEEDLE_RED);
        Path southPath = new Path();
        southPath.moveTo(centerX - needleWidth, centerY + needleBase);
        southPath.lineTo(centerX, centerY - needleLen);
        southPath.lineTo(centerX + needleWidth, centerY + needleBase);
        southPath.close();
        canvas.drawPath(southPath, needlePaint);

        // 黑色指针（北 — 指向相反方向）
        needlePaint.setColor(COLOR_NEEDLE_BLACK);
        Path northPath = new Path();
        northPath.moveTo(centerX - needleWidth, centerY - needleBase);
        northPath.lineTo(centerX, centerY + needleLen);
        northPath.lineTo(centerX + needleWidth, centerY - needleBase);
        northPath.close();
        canvas.drawPath(northPath, needlePaint);

        // 指针中心覆盖
        needlePaint.setColor(COLOR_CENTER_DOT);
        canvas.drawCircle(centerX, centerY, 5 * density, needlePaint);

        canvas.restore();
    }

    // ========================================================================
    // 13. 方位信息显示
    // ========================================================================
    private void drawDirectionText(Canvas canvas) {
        textPaint.setColor(COLOR_TEXT_WHITE);
        textPaint.setTextSize(13 * density);
        textPaint.setTextAlign(Paint.Align.LEFT);

        // 计算当前指向的24山索引
        int mountainIndex = getCurrentMountainIndex();
        String mountainName = MOUNTAINS_24[mountainIndex];
        String directionName = getDirectionName(azimuth);

        // 计算当前指向的二十八星宿
        String xiuName = getCurrentXiu();

        String info = String.format("方位: %.1f°  %s山  %s  星宿: %s",
                azimuth, mountainName, directionName, xiuName);
        canvas.drawText(info, 16 * density, 32 * density, textPaint);

        // 底部信息：三针三盘
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF605040);
        textPaint.setTextSize(9 * density);

        // 显示三盘当前指向
        int tianPanIdx = getTianPanIndex();
        int renPanIdx = getRenPanIndex();
        String tianInfo = String.format("天盘:%s  人盘:%s  地盘:%s",
                MOUNTAINS_24[tianPanIdx],
                MOUNTAINS_24[renPanIdx],
                mountainName);
        canvas.drawText(tianInfo, centerX, getHeight() - 12 * density, textPaint);
    }

    /**
     * 获取当前方位对应的24山索引（地盘正针）
     */
    private int getCurrentMountainIndex() {
        float angle = azimuth % 360;
        if (angle < 0) angle += 360;
        int index = (int) Math.round(angle / 15f) % 24;
        return index;
    }

    /**
     * 获取天盘缝针索引（+7.5°偏移）
     */
    private int getTianPanIndex() {
        float angle = (azimuth - 7.5f) % 360;
        if (angle < 0) angle += 360;
        return (int) Math.round(angle / 15f) % 24;
    }

    /**
     * 获取人盘中针索引（-7.5°偏移）
     */
    private int getRenPanIndex() {
        float angle = (azimuth + 7.5f) % 360;
        if (angle < 0) angle += 360;
        return (int) Math.round(angle / 15f) % 24;
    }

    /**
     * 获取当前指向的二十八星宿
     */
    private String getCurrentXiu() {
        float angle = azimuth % 360;
        if (angle < 0) angle += 360;

        float step = 360f / 28f;
        int closest = 0;
        float minDiff = 360;

        for (int i = 0; i < 28; i++) {
            float diff = Math.abs(angle - XIU_ANGLES[i]);
            if (diff > 180) diff = 360 - diff;
            if (diff < minDiff) {
                minDiff = diff;
                closest = i;
            }
        }
        return XIU_28[closest];
    }

    /**
     * 获取方位名称
     */
    private String getDirectionName(float degrees) {
        String[] directions = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        float[] ranges = {0, 45, 90, 135, 180, 225, 270, 315};
        String closest = directions[0];
        float minDiff = 360;
        for (int i = 0; i < ranges.length; i++) {
            float diff = Math.abs(degrees - ranges[i]);
            if (diff > 180) diff = 360 - diff;
            if (diff < minDiff) {
                minDiff = diff;
                closest = directions[i];
            }
        }
        return closest;
    }
}
