package cmd;

import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.smiles.SmilesGenerator;

/**
 * Writes the results of SMSD to text files and images.
 *  
 * @author maclean
 *
 */
public class OutputHandler {

    private ArgumentHandler argumentHandler;
    private BufferedWriter outGFile = null;
    private BufferedWriter outMFile = null;
    private BufferedWriter outDescriptorFile = null;
    private ImageGenerator imageGenerator;
    private NumberFormat nf;

    public OutputHandler(ArgumentHandler argumentHandler) {
        this.argumentHandler = argumentHandler;
        imageGenerator = new ImageGenerator();

        ////set the format right for the Tanimoto score (only two digits printed)
        nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);
    }

    public void writeQueryMol(IAtomContainer mol) throws IllegalArgumentException, IOException, CDKException {
        String suffix = argumentHandler.getSuffix();
        String qRefName = argumentHandler.getQueryMolOutName() + suffix + ".mol";
        writeMolToMolfile(mol, qRefName);
    }

    public void writeTargetMol(IAtomContainer mol) throws IllegalArgumentException, IOException, CDKException {
        String suffix = argumentHandler.getSuffix();
        String tRefName = argumentHandler.getTargetMolOutName() + suffix + ".mol";
        writeMolToMolfile(mol, tRefName);
    }

    public void writeMol(String outputType, IAtomContainer mol, String filepath) throws IOException, IllegalArgumentException, CDKException {
        Writer out;
        if (filepath.equals("--")) {
            Writer outWriter = argumentHandler.getOutputWriter();
            if (outWriter == null) {
                out = new PrintWriter(System.out);
            } else {
                out = outWriter;
            }
        } else {
            out = new FileWriter(filepath);
        }
        if (outputType.equals("MOL")) {
            writeMolToMolfile(mol, out);
        } else if (outputType.equals("SMI")) {
            writeMolToSmiles(mol, out);
        }
    }

    public void writeMolToSmiles(IAtomContainer mol, Writer out) throws IOException {
        SmilesGenerator smilesGenerator = new SmilesGenerator();
        String smiles = smilesGenerator.createSMILES(mol);
        out.write(smiles);
        out.write('\n');
        out.close();
    }

    public void writeMolToMolfile(IAtomContainer mol, String filepath) throws IOException, IllegalArgumentException, CDKException {
        Writer out;
        if (filepath.equals("--")) {
            out = new PrintWriter(System.out);
        } else {
            out = new FileWriter(filepath);
        }
        writeMolToMolfile(mol, out);
    }

    public void writeMolToMolfile(IAtomContainer mol, Writer out) throws IOException, IllegalArgumentException, CDKException {
        MDLV2000Writer writer = new MDLV2000Writer(out);
        writer.write(DefaultChemObjectBuilder.getInstance().newInstance(IMolecule.class, mol));
        writer.close();
    }

    public void startAppending(String out) throws IOException {
        String suffix = argumentHandler.getSuffix();
        outGFile = new BufferedWriter(new FileWriter(argumentHandler.getGraphFile() + suffix + out));
        outMFile = new BufferedWriter(new FileWriter(argumentHandler.getMatchFile() + suffix + out));
        outDescriptorFile = new BufferedWriter(new FileWriter(argumentHandler.getDescriptorFile() + suffix + out));
    }

    public void startNew(String out) throws IOException {
        String suffix = argumentHandler.getSuffix();
        outGFile = new BufferedWriter(new FileWriter(argumentHandler.getGraphFile() + suffix + out, true));
        outMFile = new BufferedWriter(new FileWriter(argumentHandler.getMatchFile() + suffix + out, true));

        File f = new File(argumentHandler.getDescriptorFile() + out);
        if (!f.exists()) {
            outDescriptorFile = new BufferedWriter(new FileWriter(argumentHandler.getDescriptorFile() + suffix + out, true));

            outDescriptorFile.write("Query" + "\t");
            outDescriptorFile.write("Target" + "\t");
            outDescriptorFile.write("Tanimoto (Sim.)" + "\t");
            outDescriptorFile.write("Tanimoto (Bond Sim.)" + "\t");
            outDescriptorFile.write("Tanimoto (Atom Sim.)" + "\t");
            outDescriptorFile.write("Euclidian (Dist.)" + "\t");
            outDescriptorFile.write("Cosine (Sim.)" + "\t");
            outDescriptorFile.write("Soergel (Dist.)" + "\t");
            outDescriptorFile.write("Query (Atom Count)" + "\t");
            outDescriptorFile.write("Target (Atom Count)" + "\t");
            outDescriptorFile.write("Query (Bond Count)" + "\t");
            outDescriptorFile.write("Target (Bond Count)" + "\t");
            outDescriptorFile.write("Match (Size)" + "\t");
            outDescriptorFile.write("Query (Wt.)" + "\t");
            outDescriptorFile.write("Target (Wt.)");
            outDescriptorFile.newLine();
        } else {
            outDescriptorFile = new BufferedWriter(new FileWriter(argumentHandler.getDescriptorFile() + suffix + out, true));
        }
    }

    public void writeGraphScores(String queryMolInput, String targetMolInput, double tanimotoGraph) throws IOException {
        String graphScoresL = queryMolInput + "\t" + targetMolInput + "\t" + nf.format(tanimotoGraph);
        outGFile.write(graphScoresL);
        outGFile.newLine();
    }

//    public void writeResults(IMolecule mol1, IMolecule mol2,
//            double tanimotoGraph, double tanimotoAtom, double tanimotoBond, double euclidianGraph, int nAtomsMatched,
//            long executionTime) throws IOException, CloneNotSupportedException {
    /**
     * 
     * @param mol1
     * @param mol2
     * @param tanimotoGraph
     * @param euclidianGraph
     * @param nAtomsMatched
     * @param executionTime
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    public void writeResults(IMolecule mol1, IMolecule mol2,
            double tanimotoGraph, double euclidianGraph, int nAtomsMatched,
            long executionTime) throws IOException, CloneNotSupportedException {
        String queryMolInput = argumentHandler.getQueryFilepath();
        String targetMolInput = argumentHandler.getTargetFilepath();

        double cosineGraph = 0.0;
        double SoergelGraph = 0.0;

        int mol1Size = mol1.getAtomCount();
        int mol2Size = mol2.getAtomCount();
        int mol1BondSize = mol1.getBondCount();
        int mol2BondSize = mol2.getBondCount();

        if (nAtomsMatched != 0) {
            cosineGraph = nAtomsMatched / Math.sqrt((double) mol1Size * (double) mol2Size);
            SoergelGraph = ((double) mol1Size + (double) mol2Size - 2 * nAtomsMatched)
                    / ((double) mol1Size + (double) mol2Size - nAtomsMatched);
        } else {
            tanimotoGraph = 0.0;
            euclidianGraph = 0.0;
            cosineGraph = 0.0;
            SoergelGraph = 0.0;
        }

        //to hold the graph matching descriptor
        String graphDescrptorL = queryMolInput + "\t" + targetMolInput;

        if (!argumentHandler.isAppendMode()) {
            outDescriptorFile.write(graphDescrptorL + " ");
            outDescriptorFile.write("Tanimoto (Sim.)= " + nf.format(tanimotoGraph) + " ");
//            outDescriptorFile.write("Tanimoto (Bond Sim.)= " + nf.format(tanimotoBond) + " ");
//            outDescriptorFile.write("Tanimoto (Atom Sim.)= " + nf.format(tanimotoAtom) + " ");
            outDescriptorFile.write("Euclidian (Dist.)= " + nf.format(euclidianGraph) + " ");
            outDescriptorFile.write("Cosine (Sim.)= " + nf.format(cosineGraph) + " ");
            outDescriptorFile.write("Soergel (Dist.)= " + nf.format(SoergelGraph) + " ");
            outDescriptorFile.write("Query (Atom Count)= " + mol1Size + " ");
            outDescriptorFile.write("Target (Atom Count)= " + mol2Size + " ");
            outDescriptorFile.write("Query (Bond Count)= " + mol1BondSize + " ");
            outDescriptorFile.write("Target (Bond Count)= " + mol2BondSize + " ");
            outDescriptorFile.write("Match (Size)= " + nAtomsMatched + " ");
            outDescriptorFile.write("Time (ms):" + executionTime + " ");
            outDescriptorFile.newLine();
        } else {
            outDescriptorFile.write(graphDescrptorL + "\t");
            outDescriptorFile.write(nf.format(tanimotoGraph) + "\t");
//            outDescriptorFile.write(nf.format(tanimotoBond) + "\t");
//            outDescriptorFile.write(nf.format(tanimotoAtom) + "\t");
            outDescriptorFile.write(nf.format(euclidianGraph) + "\t");
            outDescriptorFile.write(nf.format(cosineGraph) + "\t");
            outDescriptorFile.write(nf.format(SoergelGraph) + "\t");
            outDescriptorFile.write(mol1Size + "\t");
            outDescriptorFile.write(mol2Size + "\t");
            outDescriptorFile.write(mol1BondSize + "\t");
            outDescriptorFile.write(mol2BondSize + "\t");
            outDescriptorFile.write(nAtomsMatched + "\t");
            outDescriptorFile.write("Time (ms):" + executionTime + " ");
            outDescriptorFile.newLine();
            outGFile.flush();
            outMFile.flush();
            outDescriptorFile.flush();
        }
    }

    public void printHeader(
            String queryMolInput, String targetMolInput, int nAtomsMatched) throws IOException {
        outMFile.write("Molecule 1=\t" + queryMolInput);
        outMFile.newLine();
        outMFile.write("Molecule 2=\t" + targetMolInput);
        outMFile.newLine();
        outMFile.write("Max atoms matched=\t" + nAtomsMatched);
        outMFile.newLine();
    }

    public void printMapping(int solutionIndex, Map<IAtom, IAtom> mcs) throws IOException {
        outMFile.newLine();
        outMFile.write("Solution=\t" + solutionIndex);
        outMFile.newLine();
        for (Map.Entry<IAtom, IAtom> map : mcs.entrySet()) {
            String QueryIndex = map.getKey().getID();
            String TargetIndex = map.getValue().getID();
            outMFile.write(QueryIndex + "\t" + TargetIndex);
            outMFile.newLine();
        }

        outMFile.newLine();
        outMFile.write("//");
        outMFile.newLine();
    }

    public String makeLabel(double tanimotoSimilarity, double stereoScore) {
        String tanimoto = nf.format(tanimotoSimilarity);
        String stereo = nf.format(stereoScore);
        return "Scores [" + "Tanimoto: " + tanimoto + ", Stereo: " + stereo + "]";
    }

    public void printTopMapping(
            int nAtomsMatched, Map<IAtom, IAtom> mcs, Map<Integer, Integer> mcsNumber,
            String qrefName, String trefName) throws IOException {

        for (Map.Entry<IAtom, IAtom> map : mcs.entrySet()) {
            String queryIndex = map.getKey().getID();
            String targetIndex = map.getValue().getID();
            outMFile.write(queryIndex + "\t" + targetIndex);
            outMFile.newLine();
        }
        outMFile.newLine();
        outMFile.write("------------------------------------");
        outMFile.newLine();
        outMFile.write("Query =" + qrefName);
        outMFile.newLine();
        outMFile.write("Target = " + trefName);
        outMFile.newLine();
        outMFile.write("Max atoms matched=\t" + nAtomsMatched);
        outMFile.newLine();
        for (Map.Entry<Integer, Integer> map : mcsNumber.entrySet()) {
            int queryIndex = map.getKey();
            int targetIndex = map.getValue();
            outMFile.write((queryIndex + 1) + "\t" + (targetIndex + 1));
            outMFile.newLine();
        }
    }

    public void closeFiles() throws IOException {
        outGFile.close();
        outMFile.close();
        outDescriptorFile.close();
    }

    public void makeImage(IMolecule mol1, IMolecule mol2, String label, Map<Integer, Integer> mcsNumber) throws IOException, CloneNotSupportedException {
        imageGenerator.addImages(mol1, mol2, label, mcsNumber);
    }

    public void addImage(IMolecule mol1, IMolecule mol2, String label, Map<Integer, Integer> mcsNumber) throws CloneNotSupportedException, IOException {
        imageGenerator.addImages(mol1, mol2, label, mcsNumber);
    }

    public void writeImage(String qName, String tName) {
        String suffix = argumentHandler.getSuffix();
        String outImageFileName = qName + "_" + tName + suffix;
        int w = argumentHandler.getImageWidth();
        int h = argumentHandler.getImageHeight();
        if (w != - 1 && h != -1) {
            imageGenerator.createImage(outImageFileName, qName, tName, w, h);
        } else {
            imageGenerator.createImage(outImageFileName, qName, tName);
        }

    }

    public void writeCircleImage(IAtomContainer hub, List<IAtomContainer> rim, String name, List<Map<Integer, Integer>> mappings) throws IOException {
        RenderedImage image = imageGenerator.createHubWheelImage(hub, rim, mappings);
        ImageIO.write(image, "PNG", new File(name + ".png"));
    }
}
