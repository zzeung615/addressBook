package com.androidlec.addressbook.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.androidlec.addressbook.CS.CSNetworkTask;
import com.androidlec.addressbook.CS.Permission;
import com.androidlec.addressbook.JHJ_FTP.AddConnectFTP;
import com.androidlec.addressbook.R;
import com.androidlec.addressbook.StaticData;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class AddActivity extends AppCompatActivity {

    private static String name, phone, email, comment, tagListString;
    // xml
    private TextView tv_bt_register, tv_bt_cancel;
    private ImageView ivAddImage;
    private TextInputEditText et_name, et_phone, et_email, et_comment;

    // 스피너
    private String[] spinnerReNames;
    private TypedArray spinnerImages;

    // 카메라, 이미지
    private static final int IMAGE_PICK_CAMERA_CODE = 101;
    private static final int IMAGE_PICK_GALLERY_CODE = 102;
    private Uri image_uri;
    private Permission permission;

    // 태그
    private int[] iv_tags = {R.id.add_iv_tagRed, R.id.add_iv_tagOrange, R.id.add_iv_tagYellow, R.id.add_iv_tagGreen, R.id.add_iv_tagBlue, R.id.add_iv_tagPurple, R.id.add_iv_tagGray};
    private ArrayList<String> tagList;
    private int tagClick = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        // 키보드 화면 가림막기
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // 초기화
        init();

        // 클릭 리스너
        tv_bt_register.setOnClickListener(onClickListener);
        tv_bt_cancel.setOnClickListener(onClickListener);
        ivAddImage.setOnClickListener(onClickListener);

    } // onCreate

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Permission.CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(AddActivity.this, "카메라 권한을 동의해주세요.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case Permission.STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(AddActivity.this, "저장공간 권한을 동의해주세요.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    } // 권한에 대한 응답이 있을때 작동하는 함수

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case IMAGE_PICK_GALLERY_CODE:
                    image_uri = data.getData();
                case IMAGE_PICK_CAMERA_CODE:
                    uploadProfileCoverPhoto(image_uri);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    } // 카메라, 저장소에서 이미지 선택 후 이미지 주소얻기

    private void init() {
        tv_bt_register = findViewById(R.id.tvbt_addAddress_update);
        tv_bt_cancel = findViewById(R.id.tvbt_addAddress_cancel);
        ivAddImage = findViewById(R.id.iv_addAddress_image);
        et_name = findViewById(R.id.et_addAddress_name);
        et_phone = findViewById(R.id.et_addAddress_phone);
        et_email = findViewById(R.id.et_addAddress_email);
        et_comment = findViewById(R.id.et_addAddress_cmt);

        permission = new Permission(this);

        Resources res = getResources();
        spinnerReNames = MainActivity.spinnerNames;
        spinnerImages = res.obtainTypedArray(R.array.tag_array);

        spinnerReNames[0] = "태그 없음";

        for (int i = 0; i < iv_tags.length; i++) {
            final int finalI = i;
            findViewById(iv_tags[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.isSelected()) {
                        v.setSelected(false);
                        tagClick--;
                    } else {
                        if (tagClick < 3) {
                            int pos = finalI + 1;
                            v.setSelected(true);
                            Toast.makeText(AddActivity.this, spinnerReNames[pos], Toast.LENGTH_SHORT).show();
                            tagClick++;
                        } else {
                            Toast.makeText(AddActivity.this, "선택은 3개만 가능합니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            });
        }

    } // 초기화

    private void uploadProfileCoverPhoto(Uri uri) {
        Glide.with(this)
                .load(uri)
                .apply(new RequestOptions().circleCrop())
                .placeholder(R.drawable.ic_outline_emptyimage)
                .into(ivAddImage);
    } // 이미지 보이기

    private void inputNewData() {
        name = et_name.getText().toString().trim();
        phone = et_phone.getText().toString().trim();
        email = et_email.getText().toString().trim();
        comment = et_comment.getText().toString().trim();

        if (tagSelectedOK()) {
            tagListString = tagList.toString();
            tagListString = tagListString.substring(1, tagListString.length() - 1); // 앞뒤 [] 제거
            tagListString = tagListString.replace(" ", ""); // 중간 공백 제거

            // 빈칸, 유효성 검사
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "이름을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "전화번호를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            } else if (image_uri == null) {
                uploadToDB(AddActivity.this, "");
            } else {
                try {
                    AddConnectFTP addConnectFTP = new AddConnectFTP(AddActivity.this, "192.168.0.82", "host", "qwer1234", 25, image_uri);
                    addConnectFTP.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    } // 데이터 입력 액션

    private boolean tagSelectedOK() {
        tagList = new ArrayList<>();

        for (int i = 0; i < iv_tags.length; i++) {
            if (findViewById(iv_tags[i]).isSelected()) {
                int pos = i + 1;
                tagList.add(String.valueOf(i + 1));
            }
        }

        if (tagList.size() == 0) {
            tagList.add("0");
            return true;
        } else if (tagList.size() > 3) {
            Toast.makeText(this, "태그는 최대 3만 선택 가능합니다.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    } // 태그 리스트

    public static void uploadToDB(Context mContext, String fileName) {
        String urlAddr = "http://192.168.0.79:8080/test/csAddAddressBook.jsp?";

        urlAddr = urlAddr + "name=" + name + "&phone=" + phone + "&email=" + email + "&comment=" + comment + "&fileName=" + fileName + "&tags=" + tagListString + "&userId=" + StaticData.USER_ID + "&userSeq=" + StaticData.USER_SEQ;

        try {
            CSNetworkTask csNetworkTask = new CSNetworkTask(mContext, urlAddr);
            csNetworkTask.execute(); // doInBackground 의 리턴값
            Toast.makeText(mContext, name + " 연락처를 추가했습니다.", Toast.LENGTH_SHORT).show();
            ((Activity) mContext).finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // 데이터베이스에 데이터 입력

    private void showImagePicDialog() {
        String[] options = {"카메라에서 촬영", "갤러리에서 선택"};

        AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
        builder.setTitle("이미지 등록");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    if (!permission.checkCameraPermission()) {
                        permission.requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }
                } else if (which == 1) {
                    if (!permission.checkStoragePermission()) {
                        permission.requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }
                }
            }
        });
        builder.create().show();
    } // 이미지 선택 다이얼로그

    private void pickFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    } // 카메라에서 이미지선택

    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    } // 갤러리에서 이미지선택

    TextView.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.iv_addAddress_image:
                    showImagePicDialog();
                    break;
                case R.id.tvbt_addAddress_update:
                    inputNewData();
                    break;
                case R.id.tvbt_addAddress_cancel:
                    finish();
                    break;
            }
        }
    };

}