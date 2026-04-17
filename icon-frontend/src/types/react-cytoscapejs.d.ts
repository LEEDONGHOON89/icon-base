declare module "react-cytoscapejs" {
  import { Component } from "react";
  import cytoscape, { Core, ElementDefinition, LayoutOptions, Stylesheet } from "cytoscape";

  interface CytoscapeComponentProps {
    elements: ElementDefinition[];
    layout?: LayoutOptions;
    style?: React.CSSProperties;
    stylesheet?: Stylesheet[];
    cy?: (cy: Core) => void;
    zoom?: number;
    pan?: { x: number; y: number };
    minZoom?: number;
    maxZoom?: number;
    zoomingEnabled?: boolean;
    userZoomingEnabled?: boolean;
    panningEnabled?: boolean;
    userPanningEnabled?: boolean;
    boxSelectionEnabled?: boolean;
    autoungrabify?: boolean;
    autounselectify?: boolean;
    className?: string;
    id?: string;
  }

  export default class CytoscapeComponent extends Component<CytoscapeComponentProps> {}
}
