# HAWK-AI ## 1. Executive Summary This system performs real-time detection of unexploded ordnance (UXO) using a YOLOv11n model trained on the CTX-UXO dataset and deployed as an INT8-quantized TensorFlow Lite model for Android devices. The system is designed for field use, running fully offline with low latency on mobile hardware. --- ## 2. Problem Definition **Task:** Multi-class object detection **Input:** RGB image frame from device camera **Output:** Bounding boxes, class labels, and confidence scores **Operational Requirements:** - Offline inference - Real-time responsiveness - Compatibility with mobile CPU execution --- ## 3. Dataset **Dataset:** CTX-UXO – A Comprehensive Dataset for Detection and Identification of Unexploded Ordnances **Authors:** Gheorghe Marian Craioveanu, Grigore Stamatescu **Source:** IEEE Dataport **DOI:** 10.21227/cwnm-de53 ### Structure The dataset is pre-structured for object detection: dataset/ ├── train/ │ ├── images/ │ └── labels/ ├── val/ │ ├── images/ │ └── labels/ ├── test/ │ ├── images/ │ └── labels/ - **Annotations:** YOLO format (class_id, x_center, y_center, width, height) (normalized) No restructuring or relabeling was required. --- ## 4. Data Preprocessing Preprocessing is handled internally by the training pipeline: - Image resizing to **640 × 640** - Pixel normalization - On-the-fly augmentation: - Horizontal flipping - Random scaling - Small-angle rotation --- ## 5. Model Architecture **Model:** YOLOv11n YOLOv11n is a lightweight object detection model optimized for edge deployment. It uses a compact backbone and multi-scale detection heads to balance detection accuracy and computational efficiency. --- ## 6. Training Configuration **Framework:** Ultralytics YOLO **Parameters:** - Epochs: 100 - Image size: 640 - Batch size: hardware-dependent - Optimizer & LR schedule: default YOLO configuration Training uses the dataset splits directly via data.yaml. --- ## 7. Evaluation Evaluation was performed on the validation and test sets. **Metrics:** - mAP@0.5 - mAP@0.5:0.95 - Precision - Recall **Observed Behaviour:** - Strong performance on well-represented classes - Reduced sensitivity to small or ambiguous objects **Failure Cases:** - Missed detections in cluttered scenes - Confusion between visually similar UXO types --- ## 8. Model Optimization The trained model was exported to TensorFlow Lite using **INT8 quantization**. **Pipeline:** YOLO (.pt) → TensorFlow → TFLite (.tflite, INT8) **Result:** - Reduced model size - Faster inference - Slight drop in precision --- ## 9. Deployment Architecture **Mobile Inference Pipeline:** 1. Capture frame from camera 2. Resize to model input size 3. Run inference via TFLite interpreter 4. Decode output tensors 5. Render bounding boxes and labels - Runs fully offline - No API or network dependency --- ## 10. Environment Setup ### Requirements - Python 3.10+ - Ultralytics YOLO - TensorFlow ### Installation
bash
pip install ultralytics tensorflow
Training
yolo task=detect mode=train model=yolov11n.pt data=data.yaml epochs=100 imgsz=640
Export (TFLite INT8)
yolo export model=best.pt format=tflite int8=True
________________________________________
11. Artifacts
•	best.pt — trained model
•	best.tflite — quantized deployment model
•	data.yaml — dataset configuration
________________________________________
12. Limitations
•	Performance varies with lighting and image quality
•	Detection accuracy depends on dataset coverage
•	Inference performance varies across devices
________________________________________
13. Maintenance
Model updates are handled through retraining with additional data and redeployment via application updates.
