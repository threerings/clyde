--
-- $Id$

--
-- Exporter for models.
--
macroScript TRModelExporter category:"File" buttonText:"Export Model as XML..." \
    toolTip:"Export Model as XML" (

    -- Writes a point3-valued attribute to the file
    fn writePoint3Attr attr p outFile =
    (
        format "%=\"%, %, %\"" attr p.x p.y p.z to:outFile
    )

    -- Writes a quat-valued attribute to the file
    fn writeQuatAttr attr q outFile =
    (
        format "%=\"%, %, %, %\"" attr q.x q.y q.z q.w to:outFile
    )

    -- Writes a skinned vertex's bone weights to the file
    fn writeSkinBoneWeights skin vidx outFile =
    (
        weightCount = skinOps.getVertexWeightCount skin vidx
        for ii = 1 to weightCount do (
            bone = skinOps.getBoneName skin (skinOps.getVertexWeightBoneID skin vidx ii) 0
            weight = skinOps.getVertexWeight skin vidx ii
            format "      <boneWeight bone=\"%\" weight=\"%\"/>\n" bone weight to:outFile
        )
    )

    -- Writes a physiqued vertex's bone weights to the file
    fn writePhysiqueBoneWeights node vidx outFile =
    (
        boneCount = physiqueOps.getVertexBoneCount node vidx
        for ii = 1 to boneCount do (
            bone = physiqueOps.getVertexBone node vidx ii
            weight = physiqueOps.getVertexWeight node vidx ii
            format "      <boneWeight bone=\"%\" weight=\"%\"/>\n" bone.name weight to:outFile
        )
    )

    -- Writes a mesh's vertices to the file
    fn writeVertices mesh outFile =
    (
        mchannels = #()
        for ii = 2 to (meshop.getNumMaps mesh - 1) do (
            if meshop.getMapSupport mesh ii do (
                append mchannels ii
            )
        )
        for ii = 1 to mesh.numfaces do (
            face = getFace mesh ii
            normals = meshop.getFaceRNormals mesh ii
            local tvface
            if mesh.numtverts > 0 do (
                tvface = getTVFace mesh ii
            )
            mfaces = #()
            for mchannel in mchannels do (
                append mfaces (meshop.getMapFace mesh mchannel ii)
            )
            local cpvface
            if mesh.numcpvverts > 0 do (
                cpvface = getVCFace mesh ii
            )
            local alphaface
            if meshop.getMapSupport mesh -2 do (
                alphaface = meshop.getMapFace mesh -2 ii
            )
            for jj = 1 to 3 do (
                format "    <vertex" to:outFile
                writePoint3Attr " location" (getVert mesh face[jj]) outFile
				local normal
				if normals[jj] != undefined then (
					normal = normals[jj]
				) else (
					normal = getNormal mesh face[jj]
				)
	            writePoint3Attr " normal" normal outFile
                if tvface != undefined do (
                    tvert = getTVert mesh tvface[jj]
                    format " tcoords=\"%, %\"" tvert[1] tvert[2] to:outFile
                )
                if cpvface != undefined do (
                    vcolor = getVertColor mesh cpvface[jj]
                    valpha = [1, 1, 1]
                    if alphaface != undefined do (
                        valpha = meshop.getMapVert mesh -2 alphaface[jj]
                    )
                    format " color=\"%, %, %, %\"" (vcolor.r/255.0) (vcolor.g/255.0) (vcolor.b/255.0) \
                        valpha[1] to:outFile
                )
                if isProperty mesh #skin or isProperty mesh #physique or mfaces.count > 0 then (
                    format ">\n" to:outFile
                    for kk = 1 to mfaces.count do (
                        mvert = meshop.getMapVert mesh mchannels[kk] mfaces[kk][jj]
                        format "      <extra tcoords=\"%, %\"/>\n" mvert[1] mvert[2] to:outFile
                    )
                    if isProperty mesh #skin then (
                        writeSkinBoneWeights mesh.skin face[jj] outFile
                    ) else if isProperty mesh #physique then (
                        writePhysiqueBoneWeights mesh face[jj] outFile
                    )
                    format "    </vertex>\n" to:outFile
                ) else (
                    format "/>\n" to:outFile
                )
            )
        )
    )

    -- Writes a node to the file
    fn writeNode node outFile =
    (
        local kind
        isMesh = false
        if isKindOf node Editable_Mesh and not node.boneEnable then (
            isMesh = true
            if isProperty node #skin or isProperty node #physique then (
                kind = "skinMesh"
            ) else (
                kind = "triMesh"
            )
        ) else (
            kind = "node"
        )
        format "  <% name=\"%\"" kind node.name to:outFile
        xform = node.transform
        if node.parent != undefined do (
            xform = xform * (inverse node.parent.transform)
            format " parent=\"%\"" node.parent.name to:outFile
        )
        writePoint3Attr " translation" xform.translationPart outFile
        writeQuatAttr " rotation" (inverse xform.rotationPart) outFile
        writePoint3Attr " scale" xform.scalePart outFile
        if isMesh then in coordsys local (
            writePoint3Attr " offsetTranslation" node.objectOffsetPos outFile
            writeQuatAttr " offsetRotation" node.objectOffsetRot outFile
            writePoint3Attr " offsetScale" node.objectOffsetScale outFile
            if node.material != undefined and
                isKindOf node.material.diffuseMap BitmapTexture do (
                format " texture=\"%\"" (filenameFromPath node.material.diffuseMap.filename) \
                    to:outFile
            )
            tag = getUserProp node "tag"
            if tag != undefined do (
                format " tag=\"%\"" tag to:outFile
            )
            format ">\n" to:outFile
            if isProperty node #skin do (
                setCommandPanelTaskMode mode:#modify
                modPanel.setCurrentObject node.skin node:node
            )
            writeVertices node outFile
            format "  </%>\n\n" kind to:outFile

        ) else (
            format "/>\n\n" to:outFile
        )
    )

    -- Writes the model to the named file
    fn writeModel fileName =
    (
        outFile = createfile fileName
        format "<?xml version=\"1.0\" standalone=\"yes\"?>\n\n" to:outFile
        format "<model>\n\n" to:outFile
        objs = objects as array
        sels = selection as array
        if sels.count > 0 do (
            objs = sels
        )
        for node in objs do in coordsys world (
            writeNode node outFile
        )
        if sels.count > 0 do (
            select sels
        )
        format "</model>\n" to:outFile
        close outFile
    )

    --
    -- Main entry point
    --

    -- Get the target filename
    persistent global xmlModelFileName
    local fileName
    if (xmlModelFileName == undefined) then (
        fileName = maxFilePath + (getFilenameFile maxFileName) + ".mxml"
    ) else (
        fileName = xmlModelFileName
    )
    fileName = getSaveFileName caption:"Select File to Export" filename:fileName \
        types:"XML Models (*.MXML)|*.mxml|All|*.*"
    if fileName != undefined do (
        xmlModelFileName = fileName
        writeModel fileName
    )
)
