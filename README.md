# FloatingZoomView

## Usage

Step 1:
Add the JitPack repository to your project level gradle file

```groovy
allprojects {
	repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

Step 2:
Add the dependency to your application level gradle file
```groovy
	dependencies {
	        compile 'com.github.MertNYuksel:FloatingZoomView:0.1.3'
	}
```

Step 3:
Create a new instance of FloatingZoomView in your activity or fragment. FloatingZoomView will automatically add itself as content view of activity when it detects pinch zoom gesture. 
```java
ImageView originalImageView = findViewById(R.id.ivImage);
new FloatingZoomView(originalImageView);
```
