package ro.uaic.info;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Parser for CVRP instances in the TSPLIB95 / CVRPLIB format, e.g.
 * <pre>
 * NAME : P-n16-k8
 * COMMENT : (Augerat et al, No of trucks: 8, Best value: 435)
 * TYPE : CVRP
 * DIMENSION : 16
 * EDGE_WEIGHT_TYPE : EUC_2D
 * CAPACITY : 35
 * NODE_COORD_SECTION
 * 1 30 40
 * ...
 * DEMAND_SECTION
 * 1 0
 * ...
 * DEPOT_SECTION
 *  1
 *  -1
 * EOF
 * </pre>
 *
 * <p>Two dialects are understood:
 * <ul>
 *   <li>the standard one above (A, B, P, M, X sets): all nodes, depot included, are listed in
 *       {@code NODE_COORD_SECTION}; the depot must be node 1;</li>
 *   <li>the Golden / Li one: {@code EDGE_WEIGHT_TYPE: FUNCTION} with {@code EDGE_WEIGHT_FORMAT: EUC_2D},
 *       only the customers are listed and the depot is given as an {@code x y} coordinate pair in
 *       {@code DEPOT_SECTION}. Some of these instances also declare a maximum route length
 *       ({@code DISTANCE}); it is stored on the instance but not enforced by the solvers.</li>
 * </ul>
 * Only Euclidean distances are supported ({@code EUC_2D}, also assumed when no type is given).
 *
 * <p>The TSPLIB specification defines EUC_2D distances as the Euclidean distance rounded to the
 * nearest integer, and the best-known values of the A, B, P, M and X instance sets use that
 * convention. The Golden and Li instances, on the other hand, are evaluated with real-valued
 * distances. Rounding is therefore configurable: see {@link #read(Path, boolean)}.
 */
public final class VRPLibReader {

    private static final String NODE_COORD_SECTION = "NODE_COORD_SECTION";
    private static final String DEMAND_SECTION = "DEMAND_SECTION";
    private static final String DEPOT_SECTION = "DEPOT_SECTION";
    private static final String EOF = "EOF";

    private VRPLibReader() {
    }

    /** Reads an instance using real-valued (non-rounded) Euclidean distances. */
    public static VrpInstance read(Path file) throws IOException {
        return read(file, false);
    }

    /**
     * @param file           the {@code .vrp} file
     * @param roundDistances if {@code true}, round each distance to the nearest integer as TSPLIB prescribes
     */
    public static VrpInstance read(Path file, boolean roundDistances) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(reader, roundDistances);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot parse " + file + ": " + e.getMessage(), e);
        }
    }

    /** Parses an instance from its textual content. */
    public static VrpInstance parse(String content, boolean roundDistances) {
        try {
            return read(new StringReader(content), roundDistances);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static VrpInstance read(Reader source, boolean roundDistances) throws IOException {
        BufferedReader reader = source instanceof BufferedReader b ? b : new BufferedReader(source);
        Parser parser = new Parser(reader, roundDistances);
        return parser.parse();
    }

    /** Stateful single-pass parser. */
    private static final class Parser {
        private final BufferedReader reader;
        private final boolean roundDistances;

        private String name = "";
        private int dimension = -1;
        private int capacity = -1;
        private String edgeWeightType = "EUC_2D";
        private String edgeWeightFormat = "";
        private double maxRouteLength = Double.POSITIVE_INFINITY;
        /** Coordinates by node id (1-based), in file order. */
        private final Map<Integer, double[]> coordinates = new LinkedHashMap<>();
        private final Map<Integer, Integer> demand = new LinkedHashMap<>();
        private boolean coordinatesRead;
        private boolean demandRead;
        private int depotId = -1;
        private double[] depotCoordinates;
        private String pushedBack;

        Parser(BufferedReader reader, boolean roundDistances) {
            this.reader = reader;
            this.roundDistances = roundDistances;
        }

        VrpInstance parse() throws IOException {
            String line;
            while ((line = nextNonBlankLine()) != null) {
                String keyword = line.toUpperCase(Locale.ROOT);
                if (keyword.startsWith(NODE_COORD_SECTION)) {
                    readCoordinates();
                } else if (keyword.startsWith(DEMAND_SECTION)) {
                    readDemands();
                } else if (keyword.startsWith(DEPOT_SECTION)) {
                    readDepots();
                } else if (keyword.startsWith(EOF)) {
                    break;
                } else if (line.contains(":")) {
                    readHeaderEntry(line);
                } else {
                    throw new IllegalArgumentException("Unexpected line: '" + line + "'");
                }
            }
            return build();
        }

        private void readHeaderEntry(String line) {
            int colon = line.indexOf(':');
            String key = line.substring(0, colon).trim().toUpperCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            switch (key) {
                case "NAME" -> name = value;
                case "DIMENSION" -> dimension = parsePositiveInt(key, value);
                case "CAPACITY" -> capacity = parsePositiveInt(key, value);
                case "EDGE_WEIGHT_TYPE" -> edgeWeightType = value.toUpperCase(Locale.ROOT);
                case "EDGE_WEIGHT_FORMAT" -> edgeWeightFormat = value.toUpperCase(Locale.ROOT);
                case "DISTANCE" -> maxRouteLength = parseDouble(value);
                case "TYPE" -> {
                    if (!value.toUpperCase(Locale.ROOT).startsWith("CVRP")) {
                        throw new IllegalArgumentException("Unsupported TYPE '" + value + "', only CVRP is supported");
                    }
                }
                default -> {
                    // COMMENT and any other key are ignored
                }
            }
        }

        private static int parsePositiveInt(String key, String value) {
            int parsed;
            try {
                parsed = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " must be an integer, got '" + value + "'", e);
            }
            if (parsed <= 0) {
                throw new IllegalArgumentException(key + " must be positive, got " + parsed);
            }
            return parsed;
        }

        private void requireDimension(String section) {
            if (dimension < 0) {
                throw new IllegalArgumentException("DIMENSION must be declared before " + section);
            }
        }

        private void readCoordinates() throws IOException {
            requireDimension(NODE_COORD_SECTION);
            coordinatesRead = true;
            String line;
            while ((line = nextDataLine()) != null) {
                String[] fields = line.split("\\s+");
                if (fields.length < 3) {
                    throw new IllegalArgumentException("Malformed " + NODE_COORD_SECTION + " row: '" + line + "'");
                }
                int id = nodeId(fields[0]);
                if (coordinates.put(id, new double[] {parseDouble(fields[1]), parseDouble(fields[2])}) != null) {
                    throw new IllegalArgumentException("Duplicate coordinates for node " + id);
                }
            }
        }

        private void readDemands() throws IOException {
            requireDimension(DEMAND_SECTION);
            demandRead = true;
            String line;
            while ((line = nextDataLine()) != null) {
                String[] fields = line.split("\\s+");
                if (fields.length < 2) {
                    throw new IllegalArgumentException("Malformed " + DEMAND_SECTION + " row: '" + line + "'");
                }
                int id = nodeId(fields[0]);
                int value;
                try {
                    value = Integer.parseInt(fields[1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid demand '" + fields[1] + "' for node " + id, e);
                }
                if (demand.put(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate demand for node " + id);
                }
            }
        }

        /**
         * Reads either depot ids (standard dialect) or one {@code x y} coordinate pair (Golden / Li
         * dialect), terminated by {@code -1}.
         */
        private void readDepots() throws IOException {
            String line;
            while ((line = nextNonBlankLine()) != null) {
                String[] fields = line.split("\\s+");
                if (fields.length >= 2) {
                    if (depotCoordinates != null || depotId != -1) {
                        throw new IllegalArgumentException("Only a single depot is supported");
                    }
                    depotCoordinates = new double[] {parseDouble(fields[0]), parseDouble(fields[1])};
                    continue;
                }
                int id = (int) parseDouble(fields[0]);
                if (id == -1) {
                    return;
                }
                if (depotCoordinates != null || depotId != -1) {
                    throw new IllegalArgumentException("Only a single depot is supported");
                }
                depotId = id;
            }
            throw new IllegalArgumentException(DEPOT_SECTION + " is not terminated by -1");
        }

        /** Next non-blank line that starts with a digit, or null (the line is pushed back) at a section keyword. */
        private String nextDataLine() throws IOException {
            String line = nextNonBlankLine();
            if (line == null) {
                return null;
            }
            if (!Character.isDigit(line.charAt(0))) {
                pushedBack = line;
                return null;
            }
            return line;
        }

        private int nodeId(String field) {
            int id;
            try {
                id = Integer.parseInt(field);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid node id '" + field + "'", e);
            }
            if (id < 1 || id > dimension) {
                throw new IllegalArgumentException("Node id " + id + " is outside 1.." + dimension);
            }
            return id;
        }

        private static double parseDouble(String field) {
            try {
                return Double.parseDouble(field);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid coordinate '" + field + "'", e);
            }
        }

        private String nextNonBlankLine() throws IOException {
            if (pushedBack != null) {
                String line = pushedBack;
                pushedBack = null;
                return line;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    return line;
                }
            }
            return null;
        }

        private VrpInstance build() {
            if (dimension < 0) {
                throw new IllegalArgumentException("Missing DIMENSION");
            }
            if (capacity < 0) {
                throw new IllegalArgumentException("Missing CAPACITY");
            }
            if (!coordinatesRead) {
                throw new IllegalArgumentException("Missing " + NODE_COORD_SECTION);
            }
            if (!demandRead) {
                throw new IllegalArgumentException("Missing " + DEMAND_SECTION);
            }
            boolean euclidean = "EUC_2D".equals(edgeWeightType)
                    || ("FUNCTION".equals(edgeWeightType) && "EUC_2D".equals(edgeWeightFormat));
            if (!euclidean) {
                throw new IllegalArgumentException("Unsupported EDGE_WEIGHT_TYPE '" + edgeWeightType
                        + (edgeWeightFormat.isEmpty() ? "" : "' / EDGE_WEIGHT_FORMAT '" + edgeWeightFormat)
                        + "', only EUC_2D is supported");
            }

            double[][] points = new double[dimension][];
            int[] demands = new int[dimension];
            if (depotCoordinates != null) {
                // Golden / Li dialect: ids 1..dimension-1 are customers, the depot is given separately.
                if (coordinates.size() != dimension - 1) {
                    throw new IllegalArgumentException(NODE_COORD_SECTION + " lists " + coordinates.size()
                            + " customers but DIMENSION " + dimension + " implies " + (dimension - 1));
                }
                points[VrpInstance.DEPOT] = depotCoordinates;
                for (Map.Entry<Integer, double[]> e : coordinates.entrySet()) {
                    points[e.getKey()] = e.getValue();
                }
                for (Map.Entry<Integer, Integer> e : demand.entrySet()) {
                    demands[e.getKey()] = e.getValue();
                }
            } else {
                // Standard dialect: the depot is node 1.
                if (depotId != -1 && depotId != 1) {
                    throw new IllegalArgumentException("The depot must be node 1, got node " + depotId);
                }
                if (coordinates.size() != dimension) {
                    throw new IllegalArgumentException(NODE_COORD_SECTION + " lists " + coordinates.size()
                            + " nodes but DIMENSION is " + dimension);
                }
                for (Map.Entry<Integer, double[]> e : coordinates.entrySet()) {
                    points[e.getKey() - 1] = e.getValue();
                }
                for (Map.Entry<Integer, Integer> e : demand.entrySet()) {
                    demands[e.getKey() - 1] = e.getValue();
                }
            }
            for (int i = 0; i < dimension; i++) {
                if (points[i] == null) {
                    throw new IllegalArgumentException("Missing coordinates for node " + (i + 1));
                }
            }
            if (demand.size() != coordinates.size()) {
                throw new IllegalArgumentException(DEMAND_SECTION + " lists " + demand.size()
                        + " rows but " + NODE_COORD_SECTION + " lists " + coordinates.size());
            }
            return new VrpInstance(name, capacity, demands, euclideanDistances(points, roundDistances), maxRouteLength);
        }
    }

    /** Computes the symmetric Euclidean distance matrix of a set of 2D points. */
    static double[][] euclideanDistances(double[][] coordinates, boolean round) {
        int n = coordinates.length;
        double[][] distance = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double d = Math.hypot(coordinates[i][0] - coordinates[j][0], coordinates[i][1] - coordinates[j][1]);
                if (round) {
                    d = Math.rint(d);
                }
                distance[i][j] = d;
                distance[j][i] = d;
            }
        }
        return distance;
    }
}
