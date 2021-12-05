package com.kbk.capstone;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Pair;

import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassifierWithSupport {
    private static final String MODEL_NAME = "efficientnet_lite0_fp32_2.tflite";
    private static final String LABEL_FILE = "labels.txt";

    Context context;
    Model model;

    int modelInputWidth, modelInputHeight, modelInputChannel;
    int modelOutputClasses;

    TensorImage inputImage;
    TensorBuffer outputBuffer;

    private List<String> labels;

    public ClassifierWithSupport(Context context) {
        this.context = context;
    }
    private ByteBuffer loadModelFile(String modelName) throws IOException{
        AssetManager am = context.getAssets();
        AssetFileDescriptor afd = am.openFd(modelName);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel fc = fis.getChannel();
        long startOffset = afd.getStartOffset();
        long declaredLength = afd.getDeclaredLength();

        return fc.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    public void init() throws IOException{
        model = Model.createModel(context, MODEL_NAME);

        initModelShape();
        labels = FileUtil.loadLabels(context, LABEL_FILE);
        //  labels.remove(0);       label text 파일 첫번째 줄의 background 제거 라인
    }
    private void initModelShape() {
        Tensor inputTensor = model.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        modelInputChannel = inputShape[0];
        modelInputWidth = inputShape[1];
        modelInputHeight = inputShape[2];

        inputImage = new TensorImage(inputTensor.dataType());

        Tensor outputTensor = model.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType());
    }

    private Bitmap resizeBitmap(Bitmap bitmap){
        return Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, false);
    }

    private Bitmap convertBitmapToARGB8888(Bitmap bitmap){
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    private ByteBuffer convertBitmapToGrayByteBuffer(Bitmap bitmap){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixel : pixels){
            int r = pixel >> 16 & 0xFF;
            int g = pixel >> 8 & 0xFF;
            int b = pixel & 0xFF;

            float avgPixelValue = (r+g+b) / 3.0f;
            float normalizedPixelValue = avgPixelValue / 255.0f;

            byteBuffer.putFloat(normalizedPixelValue);
        }
        return byteBuffer;
    }

    public Pair<String, Float> classify(Bitmap image){
        inputImage = loadImage(image);

        Object[] inputs = new Object[]{inputImage.getBuffer()};
        Map<Integer, Object> outputs = new HashMap();
        outputs.put(0, outputBuffer.getBuffer().rewind());

        model.run(inputs, outputs);

        Map<String, Float> output = new TensorLabel(labels, outputBuffer).getMapWithFloatValue();

        return argmax(output);
    }

    private Pair<String, Float> argmax(Map<String, Float> map){
        String maxKey = "";
        float maxVal = -1;

        for(Map.Entry<String, Float> entry : map.entrySet()){
            float f = entry.getValue();
            if(f > maxVal){
                maxKey = entry.getKey();
                maxVal = f;
            }
        }
        return new Pair<>(maxKey, maxVal);
    }

    private TensorImage loadImage(final Bitmap bitmap){
        if(bitmap.getConfig() != Bitmap.Config.ARGB_8888){
            inputImage.load(convertBitmapToARGB8888(bitmap));
        }else{
            inputImage.load(bitmap);
        }

        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                    .add(new ResizeOp(modelInputWidth, modelInputHeight, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)) ///NEAREST_NEIGHBOR 교재랑 다름
                    .add(new NormalizeOp(0.0f, 255.0f))
                    .build();
        return imageProcessor.process(inputImage);
    }

    public void finish() {
        if(model != null)
            model.close();
    }
}
