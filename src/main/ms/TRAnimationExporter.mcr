--
-- $Id$

--
-- Exporter for animations.
--
macroScript TRAnimationExporter category:"File" buttonText:"Export Animation as XML..." \
    toolTip:"Export Animation as XML" (

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

    -- Writes a single node transform
    fn writeTransform node outFile = (
        format "    <transform name=\"%\"" node.name to:outFile
        xform = node.transform
        if node.parent != undefined do (
            xform = xform * (inverse node.parent.transform)
        )
        writePoint3Attr " translation" xform.translationPart outFile
        writeQuatAttr " rotation" (inverse xform.rotationPart) outFile
        writePoint3Attr " scale" xform.scalePart outFile
        format "/>\n" to:outFile
    )

    -- Writes a single animation frame
    fn writeFrame nodes outFile = (
        format "  <frame>\n" to:outFile
        for node in nodes do in coordsys world (
            writeTransform node outFile
        )
        format "  </frame>\n\n" to:outFile
    )

    -- Writes animation to the named file
    fn writeAnimation fileName =
    (
        outFile = createfile fileName
        format "<?xml version=\"1.0\" standalone=\"yes\"?>\n\n" to:outFile
        format "<animation frameRate=\"%\">\n\n" frameRate to:outFile
        objs = objects as array
        sels = selection as array
        if sels.count > 0 do (
            objs = sels
        )
        for t = animationRange.start to animationRange.end do at time t (
            writeFrame objs outFile
        )
        if sels.count > 0 do (
            select sels
        )
        format "</animation>\n" to:outFile
        close outFile
    )

    --
    -- Main entry point
    --

    -- Get the target filename
    persistent global xmlAnimFileName
    local fileName
    if (xmlAnimFileName == undefined) then (
        fileName = maxFilePath + (getFilenameFile maxFileName) + ".mxml"
    ) else (
        fileName = xmlAnimFileName
    )
    fileName = getSaveFileName caption:"Select File to Export" filename:fileName \
        types:"XML Animations (*.MXML)|*.mxml|All|*.*"
    if fileName != undefined do (
        xmlAnimFileName = fileName
        writeAnimation fileName
    )
)
