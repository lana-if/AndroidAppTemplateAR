
package com.ifpr.androidapptemplatear.ui

import androidx.fragment.app.Fragment
import com.ifpr.androidapptemplatear.R
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import okio.IOException
import java.io.File
import android.media.MediaPlayer

class CameraFragment : Fragment() {

    private lateinit var sceneView: ArSceneView
    private lateinit var modelNode: ArModelNode
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var changeButton: ExtendedFloatingActionButton

    private lateinit var sharedViewModel: SharedModelViewModel

    private val CAMERA_PERMISSION_CODE = 1001

    private var mediaPlayer: MediaPlayer? = null

    // Acessório exibido inicialmente
    private var currentModelFile = "oculos_de_corrida.glb"

    // Lista de acessórios disponíveis
    private val accessories = listOf(
        "oculos_de_corrida.glb",
        "oakley_juliet_x_metal.glb",
        "fone_static.glb",
        "anel_de_diamantes_imperio.glb"
    )

    private var accessoryIndex = 0
    private var modelLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_camera,
            container,
            false
        )

        sharedViewModel = ViewModelProvider(requireActivity())[
            SharedModelViewModel::class.java
        ]

        sceneView = view.findViewById<ArSceneView>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.DISABLED
        }

        placeButton = view.findViewById(R.id.place)
        changeButton = view.findViewById(R.id.changeAccessory)

        placeButton.setOnClickListener {
            placeModel()
        }

        changeButton.setOnClickListener {
            changeAccessory()
        }

        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        checkCameraPermission()
    }

    override fun onDestroyView() {
        mediaPlayer?.release()
        mediaPlayer = null

        super.onDestroyView()
    }

    private fun placeModel() {
        if (!modelLoaded) {
            Toast.makeText(
                requireContext(),
                "Aguarde o acessório carregar!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        modelNode.anchor()
        modelNode.isEditable = true
        modelNode.isScaleEditable = false

        sceneView.planeRenderer.isVisible = false
    }

    private fun changeAccessory() {
        accessoryIndex = (accessoryIndex + 1) % accessories.size
        currentModelFile = accessories[accessoryIndex]

        modelLoaded = false
        changeButton.isEnabled = false

        loadModel(currentModelFile)
    }

    private fun checkCameraPermission() {
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            initAR()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initAR()
        } else if (requestCode == CAMERA_PERMISSION_CODE) {
            Toast.makeText(
                requireContext(),
                "Permissão de câmera negada",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun initAR() {
        loadModel(currentModelFile)
    }

    private fun loadModel(fileName: String) {
        if (!isAdded) return

        modelLoaded = false

        if (::changeButton.isInitialized) {
            changeButton.isEnabled = false
        }

        if (::modelNode.isInitialized) {
            sceneView.removeChild(modelNode)
        }

        modelNode = ArModelNode(
            sceneView.engine,
            PlacementMode.INSTANT
        ).apply {
            isEditable = true
            isScaleEditable = false

            loadModelGlbAsync(
                glbFileLocation = fileName,
                centerOrigin = Position(0f, 0f, 1f)
            ) {
                if (!isAdded) return@loadModelGlbAsync

                modelLoaded = true

                if (::changeButton.isInitialized) {
                    changeButton.isEnabled = true
                }

                sceneView.planeRenderer.isVisible = true

                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(
                    requireContext(),
                    R.raw.peao
                )
                mediaPlayer?.start()
            }
        }

        sceneView.addChild(modelNode)
    }
}
