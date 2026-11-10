package cn.archeo.rihen;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MilestoneShareCard {
    private static final int CARD_WIDTH = 1080;
    private static final int CARD_HEIGHT = 1440;
    private static final int CARD_INSET = 54;
    private static final long STALE_CARD_AGE_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private static final int COLOR_BACKGROUND = Color.rgb(239, 238, 230);
    private static final int COLOR_PAPER = Color.rgb(255, 253, 247);
    private static final int COLOR_BORDER = Color.rgb(219, 226, 214);
    private static final int COLOR_INK = Color.rgb(24, 32, 21);
    private static final int COLOR_MUTED = Color.rgb(103, 116, 94);
    private static final int COLOR_ACCENT = Color.rgb(83, 111, 68);

    private MilestoneShareCard() {
    }

    static boolean share(Context context, LocalDate birthday, int milestone, LocalDate today) {
        if (birthday == null || !Milestones.isSpecialDay(milestone)) return false;
        if (Milestones.livedDays(birthday, today) != milestone) return false;

        try {
            Poem poem = poemFor(milestone);
            File card = drawCard(context, milestone, today, poem);
            Uri cardUri = new Uri.Builder()
                    .scheme("content")
                    .authority(context.getPackageName() + ".fileprovider")
                    .appendPath("shared_cards")
                    .appendPath(card.getName())
                    .build();

            String poemText = context.getString(poem.textRes);
            String poemSource = context.getString(poem.sourceRes);
            String subject = context.getString(
                    R.string.share_subject,
                    Integer.toString(milestone)
            );

            IntentBuilder intentBuilder = new IntentBuilder(context);
            android.content.Intent sendIntent = intentBuilder.sendIntent(
                    cardUri,
                    subject,
                    context.getString(R.string.share_text, milestone, poemText, poemSource)
            );
            android.content.Intent chooser = android.content.Intent.createChooser(
                    sendIntent,
                    context.getString(R.string.share_chooser_title)
            );
            chooser.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!(context instanceof android.app.Activity)) {
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(chooser);
            return true;
        } catch (IOException | RuntimeException | OutOfMemoryError ignored) {
            return false;
        }
    }

    private static File drawCard(Context context, int milestone, LocalDate today, Poem poem)
            throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(COLOR_BACKGROUND);
            drawFrame(canvas);
            drawHeader(canvas, context, today);
            drawMilestone(canvas, context, milestone);
            drawPoem(canvas, context, poem, today);

            File directory = new File(context.getCacheDir(), "shared_cards");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Unable to create shared card directory");
            }
            deleteStaleCards(directory);

            File output = new File(
                    directory,
                    "rihen-" + milestone + "-" + today + "-" + System.currentTimeMillis() + ".png"
            );
            try (FileOutputStream stream = new FileOutputStream(output)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw new IOException("Unable to encode shared card");
                }
            }
            return output;
        } finally {
            bitmap.recycle();
        }
    }

    private static void drawFrame(Canvas canvas) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(COLOR_PAPER);
        RectF card = new RectF(
                CARD_INSET,
                CARD_INSET,
                CARD_WIDTH - CARD_INSET,
                CARD_HEIGHT - CARD_INSET
        );
        canvas.drawRoundRect(card, 48f, 48f, fill);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(2f);
        border.setColor(COLOR_BORDER);
        canvas.drawRoundRect(card, 48f, 48f, border);
    }

    private static void drawHeader(Canvas canvas, Context context, LocalDate today) {
        Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
        mark.setColor(COLOR_ACCENT);
        mark.setStrokeWidth(5f);
        mark.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(104f, 132f, 154f, 132f, mark);

        Paint brand = textPaint(32f, COLOR_INK, Typeface.BOLD);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        brand.setLetterSpacing(0.18f);
        canvas.drawText(context.getString(R.string.card_brand), 104f, 190f, brand);

        Paint date = textPaint(28f, COLOR_MUTED, Typeface.NORMAL);
        date.setTextAlign(Paint.Align.RIGHT);
        date.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        date.setLetterSpacing(0.08f);
        canvas.drawText(
                DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.CHINA).format(today),
                CARD_WIDTH - 104f,
                176f,
                date
        );
    }

    private static void drawMilestone(Canvas canvas, Context context, int milestone) {
        Paint eyebrow = textPaint(38f, COLOR_MUTED, Typeface.NORMAL);
        eyebrow.setTextAlign(Paint.Align.CENTER);
        eyebrow.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        eyebrow.setLetterSpacing(0.18f);
        canvas.drawText(context.getString(R.string.card_eyebrow), CARD_WIDTH / 2f, 414f, eyebrow);

        Paint number = textPaint(300f, COLOR_INK, Typeface.BOLD);
        number.setTextAlign(Paint.Align.CENTER);
        number.setTypeface(Typeface.create("serif", Typeface.BOLD));
        number.setLetterSpacing(0.02f);
        String day = Integer.toString(milestone);
        while (number.measureText(day) > 760f && number.getTextSize() > 150f) {
            number.setTextSize(number.getTextSize() - 6f);
        }
        canvas.drawText(day, CARD_WIDTH / 2f, 680f, number);

        Paint unit = textPaint(36f, COLOR_ACCENT, Typeface.NORMAL);
        unit.setTextAlign(Paint.Align.CENTER);
        unit.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        canvas.drawText(context.getString(R.string.card_unit), CARD_WIDTH / 2f, 770f, unit);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(COLOR_BORDER);
        line.setStrokeWidth(2f);
        canvas.drawLine(368f, 752f, 472f, 752f, line);
        canvas.drawLine(608f, 752f, 712f, 752f, line);
    }

    private static void drawPoem(Canvas canvas, Context context, Poem poem, LocalDate today) {
        String text = context.getString(poem.textRes);
        Paint poemPaint = textPaint(54f, COLOR_INK, Typeface.NORMAL);
        poemPaint.setTextAlign(Paint.Align.CENTER);
        poemPaint.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        poemPaint.setLetterSpacing(0.04f);

        List<String> lines = wrapText(text, poemPaint, 824f);
        while (lines.size() > 4 && poemPaint.getTextSize() > 40f) {
            poemPaint.setTextSize(poemPaint.getTextSize() - 3f);
            lines = wrapText(text, poemPaint, 824f);
        }

        float top = 930f;
        float lineHeight = poemPaint.getTextSize() * 1.48f;
        for (int i = 0; i < lines.size(); i++) {
            canvas.drawText(lines.get(i), CARD_WIDTH / 2f, top + i * lineHeight, poemPaint);
        }

        float attributionBaseline = top + (lines.size() - 1) * lineHeight + 66f;
        Paint sourcePaint = textPaint(27f, COLOR_MUTED, Typeface.NORMAL);
        sourcePaint.setTextAlign(Paint.Align.CENTER);
        sourcePaint.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        sourcePaint.setLetterSpacing(0.04f);
        canvas.drawText(
                context.getString(poem.sourceRes),
                CARD_WIDTH / 2f,
                attributionBaseline,
                sourcePaint
        );

        Paint divider = new Paint(Paint.ANTI_ALIAS_FLAG);
        divider.setColor(COLOR_BORDER);
        divider.setStrokeWidth(2f);
        float dividerBaseline = Math.max(1238f, attributionBaseline + 44f);
        canvas.drawLine(420f, dividerBaseline, 660f, dividerBaseline, divider);

        String dateText = DateTimeFormatter.ofPattern(
                "yyyy\u5e74M\u6708d\u65e5 EEEE",
                Locale.CHINA
        ).format(today);
        Paint datePaint = textPaint(28f, COLOR_MUTED, Typeface.NORMAL);
        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        float dateBaseline = dividerBaseline + 50f;
        canvas.drawText(
                context.getString(R.string.card_date_line, dateText),
                CARD_WIDTH / 2f,
                dateBaseline,
                datePaint
        );

        Paint footer = textPaint(24f, COLOR_ACCENT, Typeface.NORMAL);
        footer.setTextAlign(Paint.Align.CENTER);
        footer.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        footer.setLetterSpacing(0.08f);
        canvas.drawText(
                context.getString(R.string.card_footer),
                CARD_WIDTH / 2f,
                dateBaseline + 40f,
                footer
        );
    }

    private static Paint textPaint(float size, int color, int style) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTypeface(Typeface.create(Typeface.SERIF, style));
        return paint;
    }

    private static List<String> wrapText(String text, Paint paint, float maxWidth) {
        String[] clauses = text.trim().split(
                "(?<=[\\uFF0C\\u3002\\uFF1B\\uFF1A\\uFF01\\uFF1F])"
        );
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String clause : clauses) {
            if (clause.isEmpty()) continue;
            String candidate = line + clause;
            if (line.length() > 0 && paint.measureText(candidate) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (paint.measureText(clause) <= maxWidth) {
                line.append(clause);
                continue;
            }
            if (line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }
            wrapCharacters(clause, paint, maxWidth, lines, line);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static void wrapCharacters(
            String text,
            Paint paint,
            float maxWidth,
            List<String> lines,
            StringBuilder line
    ) {
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            String candidate = line.toString() + character;
            if (line.length() > 0 && paint.measureText(candidate) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(character);
        }
    }

    private static void deleteStaleCards(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - STALE_CARD_AGE_MILLIS;
        for (File file : files) {
            if (file.isFile() && file.lastModified() < cutoff && !file.delete()) {
                file.setLastModified(System.currentTimeMillis());
            }
        }
    }

    private static Poem poemFor(int milestone) {
        switch (milestone) {
            case 100:
                return new Poem(R.string.poem_100, R.string.poem_source_100);
            case 365:
                return new Poem(R.string.poem_365, R.string.poem_source_365);
            case 500:
                return new Poem(R.string.poem_500, R.string.poem_source_500);
            case 777:
                return new Poem(R.string.poem_777, R.string.poem_source_777);
            case 1000:
                return new Poem(R.string.poem_1000, R.string.poem_source_1000);
            case 1111:
                return new Poem(R.string.poem_1111, R.string.poem_source_1111);
            case 1234:
                return new Poem(R.string.poem_1234, R.string.poem_source_1234);
            case 1500:
                return new Poem(R.string.poem_1500, R.string.poem_source_1500);
            case 2000:
                return new Poem(R.string.poem_2000, R.string.poem_source_2000);
            case 2222:
                return new Poem(R.string.poem_2222, R.string.poem_source_2222);
            case 3000:
                return new Poem(R.string.poem_3000, R.string.poem_source_3000);
            case 3333:
                return new Poem(R.string.poem_3333, R.string.poem_source_3333);
            case 4000:
                return new Poem(R.string.poem_4000, R.string.poem_source_4000);
            case 4444:
                return new Poem(R.string.poem_4444, R.string.poem_source_4444);
            case 5000:
                return new Poem(R.string.poem_5000, R.string.poem_source_5000);
            case 5555:
                return new Poem(R.string.poem_5555, R.string.poem_source_5555);
            case 6000:
                return new Poem(R.string.poem_6000, R.string.poem_source_6000);
            case 6666:
                return new Poem(R.string.poem_6666, R.string.poem_source_6666);
            case 7000:
                return new Poem(R.string.poem_7000, R.string.poem_source_7000);
            case 7777:
                return new Poem(R.string.poem_7777, R.string.poem_source_7777);
            case 8000:
                return new Poem(R.string.poem_8000, R.string.poem_source_8000);
            case 8888:
                return new Poem(R.string.poem_8888, R.string.poem_source_8888);
            case 9000:
                return new Poem(R.string.poem_9000, R.string.poem_source_9000);
            case 9999:
                return new Poem(R.string.poem_9999, R.string.poem_source_9999);
            case 10000:
                return new Poem(R.string.poem_10000, R.string.poem_source_10000);
            case 11111:
                return new Poem(R.string.poem_11111, R.string.poem_source_11111);
            case 12345:
                return new Poem(R.string.poem_12345, R.string.poem_source_12345);
            case 12999:
                return new Poem(R.string.poem_12999, R.string.poem_source_12999);
            case 15000:
                return new Poem(R.string.poem_15000, R.string.poem_source_15000);
            case 20000:
                return new Poem(R.string.poem_20000, R.string.poem_source_20000);
            case 22222:
                return new Poem(R.string.poem_22222, R.string.poem_source_22222);
            case 25000:
                return new Poem(R.string.poem_25000, R.string.poem_source_25000);
            case 30000:
                return new Poem(R.string.poem_30000, R.string.poem_source_30000);
            default:
                return new Poem(R.string.poem_default, R.string.poem_source_default);
        }
    }

    private static final class Poem {
        final int textRes;
        final int sourceRes;

        Poem(int textRes, int sourceRes) {
            this.textRes = textRes;
            this.sourceRes = sourceRes;
        }
    }

    private static final class IntentBuilder {
        private final Context context;

        IntentBuilder(Context context) {
            this.context = context;
        }

        android.content.Intent sendIntent(Uri cardUri, String subject, String text) {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(android.content.Intent.EXTRA_STREAM, cardUri);
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
            intent.setClipData(ClipData.newUri(
                    context.getContentResolver(),
                    context.getString(R.string.card_brand),
                    cardUri
            ));
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return intent;
        }
    }
}
