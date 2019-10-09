import React from "react";

interface TitleProps {
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
    <div className="tree-node-title">
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
    <div className="tree-node-nodes">
        {props.children}
    </div>
);

interface Props {
    children: [
        React.ReactElement<TitleProps>,
        React.ReactElement<NodeInfoProps>,
        React.ReactElement<OptionsProps>,
        React.ReactElement<ChildNodesProps>
    ];
}

class TreeNode extends React.PureComponent<Props> {

    static Title: typeof Title = Title;
    static NodeInfo: typeof NodeInfo = NodeInfo;
    static Options: typeof Options = Options;
    static ChildNodes: typeof ChildNodes = ChildNodes;

    render() {
        const [title, nodeInfo, options, childNodes] = this.props.children;
        return (
            <div className="tree-node-wrapper">
                {title}
                {nodeInfo}
                {options}
                {childNodes}
            </div>
        );
    }
}

export default TreeNode;
