import React from "react";

import Constants from "../Contants";

interface TitleProps {
    level: number;
    children: React.ReactNode;
}

interface NodeInfoProps {
    children: React.ReactNode;
}

interface OptionsProps {
    children: React.ReactNode;
}

interface ChildNodesProps {
    children: React.ReactNode;
}

const Title = (props: TitleProps): JSX.Element => (
    <div
        className="tree-node-title"
        style={{ paddingLeft: props.level * Constants.TREE_PADDING_OFFSET }}>
        {props.children}
    </div>
);

const NodeInfo = (props: NodeInfoProps): JSX.Element => (
    <div className="tree-node-info">
        {props.children}
    </div>
);

const Options = (props: OptionsProps): JSX.Element => (
    <div className="tree-node-options">
        {props.children}
    </div>
);

const ChildNodes = (props: ChildNodesProps): JSX.Element => (
    <div>{props.children}</div>
);

interface Props {
    children: [
        React.ReactElement<TitleProps>,
        React.ReactElement<NodeInfoProps>,
        React.ReactElement<OptionsProps>,
        React.ReactElement<ChildNodesProps>
    ];
    level: number;
}

interface State {
    isExpanded: boolean;
}

class TreeNode extends React.PureComponent<Props, State> {

    static Title: typeof Title = Title;
    static NodeInfo: typeof NodeInfo = NodeInfo;
    static Options: typeof Options = Options;
    static ChildNodes: typeof ChildNodes = ChildNodes;

    constructor(props: Props) {
        super(props);
        this.state = { isExpanded: true };
    }

    toggleChildNodes = (): void => {
        this.setState({ isExpanded: !this.state.isExpanded });
    }

    render() {
        const [title, nodeInfo, options, childNodes] = this.props.children;
        const { isExpanded } = this.state;
        return (
            <div className="tree-node-wrapper">
                <div className="tree-node-label">
                    {title}
                    {nodeInfo}
                    {options}
                </div>
                {isExpanded && childNodes}
            </div >
        );
    }
}

export default TreeNode;
